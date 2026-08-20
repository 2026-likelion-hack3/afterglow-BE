package com.afterglow.domain.onboarding.api;

import com.afterglow.domain.onboarding.domain.AgeRange;
import com.afterglow.domain.onboarding.domain.MenstrualStatus;

public record OnboardingRequest(AgeRange ageRange, MenstrualStatus menstrualStatus) {
}
