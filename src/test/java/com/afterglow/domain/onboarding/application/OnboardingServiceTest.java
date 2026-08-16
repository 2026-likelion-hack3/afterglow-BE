package com.afterglow.domain.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.onboarding.domain.AgeRange;
import com.afterglow.domain.onboarding.domain.MenstrualStatus;
import com.afterglow.domain.onboarding.domain.Onboarding;
import com.afterglow.domain.onboarding.domain.OnboardingRepository;

/**
 * OnboardingService.save는 find-or-create 후 dirty checking으로 반영되는 부분이 있어,
 * EpisodeServiceTest와 동일한 이유로 테스트 레벨 @Transactional을 붙이지 않는다 — 각 호출/재조회가
 * 독립된 트랜잭션(=독립된 persistence context)에서 실행돼야 재조회 결과가 실제 DB 상태를 그대로 반영한다.
 */
@SpringBootTest
class OnboardingServiceTest {

	@Autowired
	private OnboardingService onboardingService;

	@Autowired
	private OnboardingRepository onboardingRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Test
	void 완료_시각은_최초_제출_시점에_기록되고_재제출해도_유지된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();

		onboardingService.save(accountId, AgeRange.FIFTY_TO_FIFTY_FOUR, MenstrualStatus.IRREGULAR);
		LocalDateTime firstCompletedAt =
				onboardingRepository.findByAccountId(accountId).orElseThrow().getOnboardingCompletedAt();

		onboardingService.save(accountId, AgeRange.SIXTY_OR_OLDER, MenstrualStatus.REGULAR);
		Onboarding reloaded = onboardingRepository.findByAccountId(accountId).orElseThrow();

		assertThat(reloaded.getAgeRange()).isEqualTo(AgeRange.SIXTY_OR_OLDER);
		assertThat(reloaded.getMenstrualStatus()).isEqualTo(MenstrualStatus.REGULAR);
		assertThat(reloaded.getOnboardingCompletedAt()).isEqualTo(firstCompletedAt);
	}

	@Test
	void 익명_계정이_가입해도_동일_accountId로_저장된_온보딩_데이터가_유지된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		onboardingService.save(accountId, AgeRange.FORTY_TO_FORTY_FOUR, MenstrualStatus.REGULAR);

		Account account = accountRepository.findById(accountId).orElseThrow();
		account.verifyEmail("signup-" + accountId + "@example.com", LocalDateTime.now());
		accountRepository.save(account);

		Onboarding reloaded = onboardingRepository.findByAccountId(accountId).orElseThrow();
		assertThat(reloaded.getAgeRange()).isEqualTo(AgeRange.FORTY_TO_FORTY_FOUR);
		assertThat(reloaded.getMenstrualStatus()).isEqualTo(MenstrualStatus.REGULAR);
	}
}
