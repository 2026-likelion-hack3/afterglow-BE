package com.afterglow.domain.onboarding.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.afterglow.domain.account.domain.AccountDeletedEvent;
import com.afterglow.domain.onboarding.domain.OnboardingRepository;

import lombok.RequiredArgsConstructor;

/**
 * 계정 삭제와 온보딩 삭제가 하나의 트랜잭션으로 원자적이어야 하므로(둘 중 하나만 반영되면 안 됨) BEFORE_COMMIT을
 * 쓴다 — 물리 커밋 직전에 실행되어 AccountService.deleteAccount의 트랜잭션에 그대로 포함되고, 여기서 예외가
 * 나면 계정 삭제까지 함께 롤백된다. AFTER_COMMIT이나 비동기 처리는 계정 삭제가 이미 커밋된 뒤라 온보딩 삭제
 * 실패를 되돌릴 수 없어 쓰지 않는다.
 */
@Component
@RequiredArgsConstructor
public class OnboardingAccountDeletedListener {

	private final OnboardingRepository onboardingRepository;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onAccountDeleted(AccountDeletedEvent event) {
		onboardingRepository.deleteByAccountId(event.accountId());
	}
}
