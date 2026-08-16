package com.afterglow.domain.onboarding.api;

import java.time.LocalDateTime;

import com.afterglow.domain.onboarding.domain.AgeRange;
import com.afterglow.domain.onboarding.domain.MenstrualStatus;

public record OnboardingResponse(AgeRange ageRange, MenstrualStatus menstrualStatus, LocalDateTime onboardingCompletedAt) {
}
