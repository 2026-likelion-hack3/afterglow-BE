package com.afterglow.account.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.afterglow.account.domain.EmailSender;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Component
@Profile("prod")
public class SesEmailSender implements EmailSender {

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
		sesClient.sendEmail(request);
	}
}
