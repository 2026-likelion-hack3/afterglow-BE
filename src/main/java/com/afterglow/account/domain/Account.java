package com.afterglow.account.domain;

import java.time.LocalDateTime;

import com.afterglow.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기기 최초 실행 시 이메일 없이 생성되는 익명 계정으로 시작한다. 이메일 인증을 완료하면 이 계정에 이메일이 연결되며
 * (별도의 계정을 새로 만들지 않는다), 그 시점부터 다른 기기에서도 같은 계정으로 로그인할 수 있다.
 */
@Getter
@Entity
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {

	@Column(unique = true)
	private String email;

	private LocalDateTime emailVerifiedAt;

	public static Account createAnonymous() {
		return new Account();
	}

	public boolean isEmailVerified() {
		return emailVerifiedAt != null;
	}

	public void verifyEmail(String email, LocalDateTime verifiedAt) {
		this.email = email;
		this.emailVerifiedAt = verifiedAt;
	}
}
