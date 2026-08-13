package com.afterglow.domain.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.account.domain.EmailSender;
import com.afterglow.domain.account.domain.VerificationPurpose;
import com.afterglow.domain.account.infrastructure.EmailVerificationJpaRepository;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

/**
 * AccountService가 클래스 레벨 @Transactional이라 issueCode 내부에서 저장한 EmailVerification은
 * emailSender 호출이 실패(RuntimeException)하면 같은 트랜잭션이 롤백되어 커밋되지 않는다.
 * 이 테스트는 그 구조를 바꾸지 않고, 실제로 롤백되는지만 블랙박스로 검증한다 — 그래서 테스트 메서드
 * 자체는 의도적으로 @Transactional을 붙이지 않는다(그러면 AccountService 호출이 자기 자신의
 * 트랜잭션 경계를 갖게 되어, 저장 후 실패 시 실제 커밋되지 않았는지를 그대로 관찰할 수 있다).
 */
@SpringBootTest
class AccountServiceRollbackTest {

	@Autowired
	private AccountService accountService;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private EmailVerificationJpaRepository emailVerificationJpaRepository;

	@MockBean
	private EmailSender emailSender;

	@Test
	void 이메일_발송이_실패하면_저장된_인증코드가_커밋되지_않는다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		String email = "rollback-" + UUID.randomUUID() + "@example.com";
		doThrow(new AfterglowException(ErrorCode.EMAIL_SEND_FAILED))
				.when(emailSender).sendVerificationCode(anyString(), anyString());

		assertThatThrownBy(() -> accountService.requestSignupVerificationCode(accountId, email))
				.isInstanceOf(AfterglowException.class);

		boolean saved = emailVerificationJpaRepository.findAll().stream()
				.anyMatch(v -> v.getEmail().equals(email) && v.getPurpose() == VerificationPurpose.SIGNUP);
		assertThat(saved).isFalse();
	}
}
