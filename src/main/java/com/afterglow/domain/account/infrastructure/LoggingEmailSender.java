package com.afterglow.domain.account.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.afterglow.domain.account.domain.EmailSender;

/** local/test에서 쓰는 구현 — 실제 메일 발송 없이 로그로만 코드를 남긴다. prod는 SesEmailSender를 쓴다. */
@Component
@Profile("!prod")
public class LoggingEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

	@Override
	public void sendVerificationCode(String email, String code) {
		log.info("[STUB][이메일 미발송] {} 인증코드: {}", email, code);
	}
}
