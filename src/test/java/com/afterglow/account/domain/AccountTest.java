package com.afterglow.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AccountTest {

	@Test
	void 익명_계정은_이메일이_없고_인증되지_않은_상태로_생성된다() {
		Account account = Account.createAnonymous();

		assertThat(account.getEmail()).isNull();
		assertThat(account.isEmailVerified()).isFalse();
	}

	@Test
	void 이메일을_인증하면_이메일과_인증시각이_설정된다() {
		Account account = Account.createAnonymous();
		LocalDateTime verifiedAt = LocalDateTime.now();

		account.verifyEmail("user@example.com", verifiedAt);

		assertThat(account.getEmail()).isEqualTo("user@example.com");
		assertThat(account.getEmailVerifiedAt()).isEqualTo(verifiedAt);
		assertThat(account.isEmailVerified()).isTrue();
	}
}
