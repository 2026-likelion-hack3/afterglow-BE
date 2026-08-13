package com.afterglow.domain.account.domain;

import java.util.Optional;

public interface EmailVerificationRepository {

	EmailVerification save(EmailVerification emailVerification);

	Optional<EmailVerification> findTopByEmailAndPurposeAndCodeOrderByCreatedAtDesc(
			String email, VerificationPurpose purpose, String code);
}
