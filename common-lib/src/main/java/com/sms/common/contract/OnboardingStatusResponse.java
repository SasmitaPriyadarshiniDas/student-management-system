package com.sms.common.contract;

import java.io.Serializable;
import java.util.UUID;

public record OnboardingStatusResponse(UUID studentId, OnboardingStatus status) implements Serializable {
}
