package com.afterglow.domain.onboarding.domain;

import java.util.Optional;

public interface OnboardingRepository {

	Onboarding save(Onboarding onboarding);

	Optional<Onboarding> findByAccountId(Long accountId);

	void deleteByAccountId(Long accountId);
}
