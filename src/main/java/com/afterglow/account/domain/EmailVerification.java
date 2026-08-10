package com.afterglow.account.domain;

import java.time.LocalDateTime;

import com.afterglow.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 이메일로 발송한 6자리 인증코드 한 건. 재요청 시 이전 코드를 지우지 않고 새 코드를 추가로 저장한다 — 검증은 코드값 자체로 일치시킨다. */
@Getter
@Entity
@Table(name = "email_verification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String code;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private VerificationPurpose purpose;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	private LocalDateTime consumedAt;

	@Builder
	private EmailVerification(String email, String code, VerificationPurpose purpose, LocalDateTime expiresAt) {
		this.email = email;
		this.code = code;
		this.purpose = purpose;
		this.expiresAt = expiresAt;
	}

	public static EmailVerification create(String email, String code, VerificationPurpose purpose, LocalDateTime expiresAt) {
		return EmailVerification.builder()
				.email(email)
				.code(code)
				.purpose(purpose)
				.expiresAt(expiresAt)
				.build();
	}

	public boolean isExpired(LocalDateTime now) {
		return expiresAt.isBefore(now);
	}

	public boolean isConsumed() {
		return consumedAt != null;
	}

	public void consume() {
		this.consumedAt = LocalDateTime.now();
	}
}
