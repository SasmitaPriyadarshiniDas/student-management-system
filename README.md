# Student Management System

Spring Boot 3.3 / Java 21 microservices matching the architecture diagram.

```
Web/Mobile App ──► Azure Front Door (WAF) ──► API Gateway (Spring Cloud Gateway)
        │                                         │  validates Entra ID JWT, routes by path
        └── sign-in ──► Microsoft Entra ID ◄──────┘
                                                  ├─► student-service    ─► Redis + PostgreSQL (student_db)
                                                  ├─► document-service   ─► Redis + PostgreSQL (document_db) + Azure Blob Storage
                                                  └─► enrollment-service ─► Redis + PostgreSQL (enrollment_db)
```

| Module | Port | Responsibility |
|---|---|---|
| `common-lib` | – | JWT validation (issuer + audience), role mapping, error format, Redis cache config, token-relay client for student-service |
| `api-gateway` | 8080 | Only public entry point. Rejects requests without a valid token, routes `/api/students`, `/api/documents`, `/api/courses`, `/api/enrollments`, CORS |
| `student-service` | 8081 | Student profile + onboarding status (`PENDING_DOCUMENTS → DOCUMENTS_VERIFIED → ACTIVE`) |
| `document-service` | 8082 | Upload to Blob Storage, admin verify/reject. When all required document types are verified it moves the student to `DOCUMENTS_VERIFIED` |
| `enrollment-service` | 8083 | Course catalog, seat-safe enrollment (atomic seat reservation), cancellation. Requires onboarding to be complete |

Design notes
- Student id = the caller's Entra **object id (`oid`)**, so no id lookup is needed between services.
- Each service owns its database and its Redis cache and uses cache-aside (`@Cacheable` / `@CacheEvict`).
- Services re-validate the JWT; service-to-service calls relay the caller's bearer token.
- Last seat race: `UPDATE courses SET enrolled_count = enrolled_count + 1 WHERE id = ? AND enrolled_count < capacity`.
- Schema is managed by Flyway (`src/main/resources/db/migration`), Hibernate only validates.

## 1. Entra ID setup (once)

1. **App registration "SMS API"** → *Expose an API* → set Application ID URI (default `api://<client-id>` is fine).
   Add a scope, e.g. `access_as_user`.
2. In its **Manifest** set `"requestedAccessTokenVersion": 2` (so tokens have `aud = <client-id>` and the v2 issuer).
3. **App roles**: create `Admin` and `Student` (allowed member types: Users/Groups), then assign them to users in *Enterprise applications*.
4. **App registration "SMS Web"** (your front end, SPA platform) → API permissions → add the `access_as_user` scope.
5. Put the tenant id and the *SMS API* client id into `.env` (`AZURE_TENANT_ID`, `API_AUDIENCE`).

## 2. Run locally

```bash
cp .env.example .env        # fill in AZURE_TENANT_ID and API_AUDIENCE
docker compose up --build   # postgres, redis, azurite (blob emulator) + all services
```

Gateway: http://localhost:8080 · health: `/actuator/health`

Get a token (Azure CLI must be pre-authorised on the API, or use your SPA / Postman with the auth-code flow):

```bash
TOKEN=$(az account get-access-token --scope api://<API_CLIENT_ID>/access_as_user --query accessToken -o tsv)

# 1. create my profile
curl -X POST localhost:8080/api/students -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"firstName":"Asha","lastName":"Rao","dateOfBirth":"2004-05-17","phone":"+91 9000000000"}'

# 2. upload documents
curl -X POST localhost:8080/api/documents -H "Authorization: Bearer $TOKEN" -F type=ID_PROOF -F file=@id.pdf
curl -X POST localhost:8080/api/documents -H "Authorization: Bearer $TOKEN" -F type=TRANSCRIPT -F file=@marks.pdf

# 3. (admin token) verify each document → student becomes DOCUMENTS_VERIFIED
curl -X PUT localhost:8080/api/documents/<docId>/verify -H "Authorization: Bearer $ADMIN_TOKEN"

# 4. (admin) create a course, then (student) enroll
curl -X POST localhost:8080/api/courses -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"code":"CS101","title":"Intro to CS","description":"Basics","capacity":30}'
curl -X POST localhost:8080/api/enrollments -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"courseId":"<courseId>"}'
```

Without Docker (needs Java 21, Maven, and local Postgres/Redis/Azurite): `mvn clean package`, then run each module with
`mvn -pl student-service spring-boot:run` etc. Create the three databases from `infra/postgres/init.sql` first.

## 3. API summary

| Method & path | Who | Notes |
|---|---|---|
| `POST /api/students` | any user | creates own profile |
| `GET/PUT /api/students/me` | any user | |
| `GET /api/students`, `PUT /api/students/{id}/onboarding-status` | Admin | paged list / manual status change |
| `GET /api/students/{id}`, `/{id}/onboarding-status` | self or Admin | |
| `POST /api/documents` (multipart `type`, `file`) | any user | PDF/JPEG/PNG, max 10 MB |
| `GET /api/documents/me`, `/student/{id}`, `/{id}`, `/{id}/content` | owner or Admin | content = file download |
| `PUT /api/documents/{id}/verify`, `/{id}/reject` | Admin | reject body `{"reason":"..."}` |
| `GET /api/courses`, `/{id}` | any user | cached |
| `POST/PUT /api/courses` | Admin | |
| `POST /api/enrollments`, `GET /api/enrollments/me`, `DELETE /api/enrollments/{id}` | any user | enrolling needs completed onboarding |

Errors are RFC 7807 JSON (`application/problem+json`).

## 4. Deploying to Azure

- Containers: build with `docker build --build-arg MODULE=<module> .` → Azure Container Registry → Azure Container Apps / AKS.
  Only the gateway needs external ingress; put Front Door (with WAF) in front of it.
- PostgreSQL: Azure Database for PostgreSQL Flexible Server (one database per service). Set `DB_URL`, `DB_USER`, `DB_PASSWORD` (Key Vault references).
- Redis: Azure Cache for Redis → `REDIS_HOST`, `REDIS_PORT=6380`, `REDIS_SSL=true`, `SPRING_DATA_REDIS_PASSWORD`.
- Blob Storage: leave `STORAGE_CONNECTION_STRING` empty, set `STORAGE_ENDPOINT=https://<account>.blob.core.windows.net`, give the
  document-service managed identity the *Storage Blob Data Contributor* role.
- Service URLs: set `STUDENT_SERVICE_URL`, `DOCUMENT_SERVICE_URL`, `ENROLLMENT_SERVICE_URL` to the internal Container Apps FQDNs.
- Choose one gateway implementation: this code base uses Spring Cloud Gateway. If you prefer Azure APIM, drop `api-gateway`
  and point APIM at the three services with the same JWT validation policy (issuer + audience).

## Not included / next steps

Front-end app, IaC (Bicep/Terraform), CI pipeline, integration tests with Testcontainers, rate limiting, distributed tracing (OpenTelemetry).
