package com.afterglow.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class EmailVerificationTest {

	@Test
	void 만료_시각_이전이면_만료되지_않은_것으로_판단한다() {
		EmailVerification verification = EmailVerification.create(
				"user@example.com", "123456", VerificationPurpose.SIGNUP, LocalDateTime.now().plusMinutes(5));

		assertThat(verification.isExpired(LocalDateTime.now())).isFalse();
	}

	@Test
	void 만료_시각_이후면_만료된_것으로_판단한다() {
		EmailVerification verification = EmailVerification.create(
				"user@example.com", "123456", VerificationPurpose.SIGNUP, LocalDateTime.now().minusMinutes(1));

		assertThat(verification.isExpired(LocalDateTime.now())).isTrue();
	}

	@Test
	void 생성_직후에는_소비되지_않은_상태다() {
		EmailVerification verification = EmailVerification.create(
				"user@example.com", "123456", VerificationPurpose.SIGNUP, LocalDateTime.now().plusMinutes(5));

		assertThat(verification.isConsumed()).isFalse();
	}

	@Test
	void consume을_호출하면_소비된_상태가_된다() {
		EmailVerification verification = EmailVerification.create(
				"user@example.com", "123456", VerificationPurpose.SIGNUP, LocalDateTime.now().plusMinutes(5));

		verification.consume();

		assertThat(verification.isConsumed()).isTrue();
	}
}
