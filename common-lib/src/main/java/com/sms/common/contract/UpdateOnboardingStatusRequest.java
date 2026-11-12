package com.sms.common.contract;

import jakarta.validation.constraints.NotNull;

public record UpdateOnboardingStatusRequest(@NotNull OnboardingStatus status) {
}
