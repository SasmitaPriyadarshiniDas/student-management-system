package com.sms.common.client;

import com.sms.common.contract.OnboardingStatus;
import com.sms.common.contract.OnboardingStatusResponse;
import com.sms.common.contract.UpdateOnboardingStatusRequest;
import com.sms.common.web.ForbiddenException;
import com.sms.common.web.NotFoundException;
import com.sms.common.web.UpstreamException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Synchronous client for student-service. Only created in services that set
 * sms.clients.student-service.url (document-service, enrollment-service).
 */
@Component
@ConditionalOnProperty("sms.clients.student-service.url")
public class StudentServiceClient {

    private final RestClient rest;

    public StudentServiceClient(@Value("${sms.clients.student-service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.rest = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor(new BearerTokenRelayInterceptor())
                .build();
    }

    public OnboardingStatus getOnboardingStatus(UUID studentId) {
        try {
            OnboardingStatusResponse body = rest.get()
                    .uri("/api/students/{id}/onboarding-status", studentId)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new NotFoundException("Student profile not found. Create your profile first.");
                    })
                    .onStatus(s -> s.value() == 403, (req, res) -> {
                        throw new ForbiddenException("Not allowed to read this student's onboarding status");
                    })
                    .body(OnboardingStatusResponse.class);
            if (body == null) {
                throw new UpstreamException("student-service returned an empty response");
            }
            return body.status();
        } catch (RestClientException e) {
            throw new UpstreamException("student-service is unavailable", e);
        }
    }

    public void updateOnboardingStatus(UUID studentId, OnboardingStatus status) {
        try {
            rest.put()
                    .uri("/api/students/{id}/onboarding-status", studentId)
                    .body(new UpdateOnboardingStatusRequest(status))
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new NotFoundException("Student profile not found");
                    })
                    .onStatus(s -> s.value() == 403, (req, res) -> {
                        throw new ForbiddenException("Not allowed to change onboarding status");
                    })
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new UpstreamException("student-service is unavailable", e);
        }
    }
}
