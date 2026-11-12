package com.sms.enrollment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @Min(1) @Max(10000) int capacity) {
}
