package com.afterglow.domain.account.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

@ExtendWith(MockitoExtension.class)
class SesEmailSenderTest {

	@Mock
	private SesClient sesClient;

	private SesEmailSender sesEmailSender;

	@BeforeEach
	void setUp() {
		sesEmailSender = new SesEmailSender(sesClient, "sender@example.com");
	}

	@Test
	void 정상_발송이면_sesClient의_sendEmail이_호출된다() {
		sesEmailSender.sendVerificationCode("user@example.com", "123456");

		verify(sesClient).sendEmail(any(SendEmailRequest.class));
	}

	@Test
	void SesException이_발생하면_EMAIL_SEND_FAILED로_변환된다() {
		when(sesClient.sendEmail(any(SendEmailRequest.class)))
				.thenThrow(SesException.builder().message("발신자 이메일이 인증되지 않았습니다").build());

		assertThatThrownBy(() -> sesEmailSender.sendVerificationCode("user@example.com", "123456"))
				.isInstanceOf(AfterglowException.class)
				.satisfies(e -> assertThat(((AfterglowException) e).getErrorCode()).isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
	}

	@Test
	void SdkClientException이_발생하면_EMAIL_SEND_FAILED로_변환된다() {
		when(sesClient.sendEmail(any(SendEmailRequest.class)))
				.thenThrow(SdkClientException.create("네트워크 오류"));

		assertThatThrownBy(() -> sesEmailSender.sendVerificationCode("user@example.com", "123456"))
				.isInstanceOf(AfterglowException.class)
				.satisfies(e -> assertThat(((AfterglowException) e).getErrorCode()).isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
	}
}
