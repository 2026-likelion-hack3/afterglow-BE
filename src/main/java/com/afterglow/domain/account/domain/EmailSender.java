package com.afterglow.domain.account.domain;

public interface EmailSender {

	void sendVerificationCode(String email, String code);
}
