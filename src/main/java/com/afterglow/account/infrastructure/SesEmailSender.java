package com.afterglow.account.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.afterglow.account.domain.EmailSender;
import com.afterglow.common.exception.AfterglowException;
import com.afterglow.common.exception.ErrorCode;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

@Component
@Profile("prod")
public class SesEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(SesEmailSender.class);

	private final SesClient sesClient;
	private final String senderEmail;

	public SesEmailSender(SesClient sesClient, @Value("${afterglow.aws.ses.sender-email}") String senderEmail) {
		this.sesClient = sesClient;
		this.senderEmail = senderEmail;
	}

	@Override
	public void sendVerificationCode(String email, String code) {
		SendEmailRequest request = SendEmailRequest.builder()
				.source(senderEmail)
				.destination(Destination.builder().toAddresses(email).build())
				.message(Message.builder()
						.subject(Content.builder().data("[Afterglow] 인증코드").build())
						.body(Body.builder()
								.text(Content.builder().data("인증코드: " + code + " (5분 이내에 입력해주세요)").build())
								.build())
						.build())
				.build();
		try {
			sesClient.sendEmail(request);
		} catch (SesException | SdkClientException e) {
			log.error("SES 이메일 발송 실패 (수신자: {})", mask(email), e);
			throw new AfterglowException(ErrorCode.EMAIL_SEND_FAILED);
		}
	}

	/** 로그에 이메일 원문을 그대로 남기지 않기 위한 최소 마스킹. */
	private String mask(String email) {
		int at = email.indexOf('@');
		if (at <= 1) {
			return "***" + email.substring(at);
		}
		return email.substring(0, 2) + "***" + email.substring(at);
	}
}
