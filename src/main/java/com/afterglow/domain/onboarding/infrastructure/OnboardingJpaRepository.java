package com.afterglow.domain.onboarding.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.domain.onboarding.domain.Onboarding;
import com.afterglow.domain.onboarding.domain.OnboardingRepository;

public interface OnboardingJpaRepository extends JpaRepository<Onboarding, Long>, OnboardingRepository {
}
