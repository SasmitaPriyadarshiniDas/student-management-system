CREATE TABLE students (
    id                UUID PRIMARY KEY,                 -- Entra object id (oid)
    email             VARCHAR(320) NOT NULL UNIQUE,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    date_of_birth     DATE         NOT NULL,
    phone             VARCHAR(30),
    onboarding_status VARCHAR(30)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_students_last_name ON students (last_name, first_name);
