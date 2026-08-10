package com.afterglow.account.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.afterglow.account.domain.EmailSender;

/** SES 연동 전까지 쓰는 임시 구현 — 실제 메일 발송 없이 로그로만 코드를 남긴다. SES 붙이면 이 구현으로 교체한다. */
@Component
public class LoggingEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

	@Override
	public void sendVerificationCode(String email, String code) {
		log.info("[STUB][이메일 미발송] {} 인증코드: {}", email, code);
	}
}
