package com.sms.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** Rejects tokens that were not issued for this API (aud claim must match one of the configured audiences). */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> allowed;

    public AudienceValidator(List<String> allowed) {
        this.allowed = allowed;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> aud = jwt.getAudience();
        if (aud != null && aud.stream().anyMatch(allowed::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "The token audience is not accepted by this API", null));
    }
}
