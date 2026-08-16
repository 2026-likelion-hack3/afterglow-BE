package com.afterglow.domain.onboarding.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class OnboardingTest {

	@Test
	void 생성_직후에는_모든_답변과_완료_시각이_비어있다() {
		Onboarding onboarding = Onboarding.createFor(1L);

		assertThat(onboarding.getAccountId()).isEqualTo(1L);
		assertThat(onboarding.getAgeRange()).isNull();
		assertThat(onboarding.getMenstrualStatus()).isNull();
		assertThat(onboarding.getOnboardingCompletedAt()).isNull();
	}

	@Test
	void 최초_제출_시_완료_시각이_기록된다() {
		Onboarding onboarding = Onboarding.createFor(1L);
		LocalDateTime now = LocalDateTime.now();

		onboarding.submitAnswers(AgeRange.FIFTY_TO_FIFTY_FOUR, MenstrualStatus.IRREGULAR, now);

		assertThat(onboarding.getAgeRange()).isEqualTo(AgeRange.FIFTY_TO_FIFTY_FOUR);
		assertThat(onboarding.getMenstrualStatus()).isEqualTo(MenstrualStatus.IRREGULAR);
		assertThat(onboarding.getOnboardingCompletedAt()).isEqualTo(now);
	}

	@Test
	void 전체_스킵으로도_제출할_수_있고_완료_시각은_기록된다() {
		Onboarding onboarding = Onboarding.createFor(1L);
		LocalDateTime now = LocalDateTime.now();

		onboarding.submitAnswers(null, null, now);

		assertThat(onboarding.getAgeRange()).isNull();
		assertThat(onboarding.getMenstrualStatus()).isNull();
		assertThat(onboarding.getOnboardingCompletedAt()).isEqualTo(now);
	}

	@Test
	void 재제출해도_완료_시각은_최초_값을_유지한다() {
		Onboarding onboarding = Onboarding.createFor(1L);
		LocalDateTime first = LocalDateTime.now().minusDays(1);
		onboarding.submitAnswers(AgeRange.FIFTY_TO_FIFTY_FOUR, MenstrualStatus.IRREGULAR, first);

		onboarding.submitAnswers(AgeRange.SIXTY_OR_OLDER, MenstrualStatus.REGULAR, LocalDateTime.now());

		assertThat(onboarding.getAgeRange()).isEqualTo(AgeRange.SIXTY_OR_OLDER);
		assertThat(onboarding.getMenstrualStatus()).isEqualTo(MenstrualStatus.REGULAR);
		assertThat(onboarding.getOnboardingCompletedAt()).isEqualTo(first);
	}
}
