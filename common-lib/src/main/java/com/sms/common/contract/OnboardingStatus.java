package com.sms.common.contract;

/** Onboarding lifecycle of a student. Owned by student-service, read by the other services. */
public enum OnboardingStatus {
    PENDING_DOCUMENTS,
    DOCUMENTS_VERIFIED,
    ACTIVE
}
