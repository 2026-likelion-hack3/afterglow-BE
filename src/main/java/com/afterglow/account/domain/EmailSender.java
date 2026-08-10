package com.afterglow.account.domain;

public interface EmailSender {

	void sendVerificationCode(String email, String code);
}
