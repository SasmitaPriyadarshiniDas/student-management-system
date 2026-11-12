package com.sms.common.security;

import com.sms.common.web.ForbiddenException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Helpers for reading the caller's identity out of an Entra ID access token. */
public final class AuthUser {

    private AuthUser() {
    }

    /** The Entra object id (oid) is used as the student's primary key everywhere. */
    public static UUID id(Jwt jwt) {
        String value = jwt.getClaimAsString("oid");
        if (value == null) {
            value = jwt.getSubject();
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ForbiddenException("Token does not contain a usable user identifier (oid)");
        }
    }

    public static String email(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return email != null ? email : jwt.getClaimAsString("preferred_username");
    }

    public static Set<String> roles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return Set.of();
        }
        return roles.stream().map(r -> r.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    public static boolean isAdmin(Jwt jwt) {
        return roles(jwt).contains("ADMIN");
    }

    public static void requireSelfOrAdmin(Jwt jwt, UUID ownerId) {
        if (!isAdmin(jwt) && !id(jwt).equals(ownerId)) {
            throw new ForbiddenException("You are not allowed to access this resource");
        }
    }
}
