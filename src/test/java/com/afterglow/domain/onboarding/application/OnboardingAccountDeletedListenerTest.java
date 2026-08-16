package com.afterglow.domain.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.afterglow.domain.account.application.AccountService;
import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.onboarding.domain.OnboardingRepository;

/**
 * 계정 삭제(AccountService.deleteAccount)와 온보딩 삭제(OnboardingAccountDeletedListener)가 하나의
 * 트랜잭션으로 원자적인지 검증한다. onboardingRepository.deleteByAccountId가 실패하면 BEFORE_COMMIT
 * 리스너의 예외가 같은 트랜잭션을 롤백시켜, 이미 EntityManager에서 remove()된 Account도 커밋되지 않고
 * 그대로 남아있어야 한다.
 */
@SpringBootTest
class OnboardingAccountDeletedListenerTest {

	@Autowired
	private AccountService accountService;

	@Autowired
	private AccountRepository accountRepository;

	@MockBean
	private OnboardingRepository onboardingRepository;

	@Test
	void 온보딩_삭제가_실패하면_계정_삭제도_롤백된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		doThrow(new RuntimeException("온보딩 삭제 실패")).when(onboardingRepository).deleteByAccountId(anyLong());

		assertThatThrownBy(() -> accountService.deleteAccount(accountId))
				.isInstanceOf(RuntimeException.class);

		assertThat(accountRepository.findById(accountId)).isPresent();
	}
}
