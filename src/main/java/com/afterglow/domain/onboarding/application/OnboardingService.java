package com.afterglow.domain.onboarding.application;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.onboarding.domain.AgeRange;
import com.afterglow.domain.onboarding.domain.MenstrualStatus;
import com.afterglow.domain.onboarding.domain.Onboarding;
import com.afterglow.domain.onboarding.domain.OnboardingRepository;
import com.afterglow.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingService {

	private final OnboardingRepository onboardingRepository;
	private final AccountRepository accountRepository;

	/** 계정 존재 확인 + 단건 조회, 둘 다 단순 SELECT뿐이고 그 사이 원자성이 필요한 변경이 없어 @Transactional을 두지 않는다. */
	public Onboarding get(Long accountId) {
		requireAccountExists(accountId);
		return onboardingRepository.findByAccountId(accountId).orElse(null);
	}

	/**
	 * 최초 호출이면 row를 새로 만들고, 이후 호출이면 기존 row를 찾아 답변을 덮어쓴다(find-or-create +
	 * dirty checking). 조회/생성/변경이 하나의 persistence context 안에서 이어져야 하므로 @Transactional이 필요하다.
	 */
	@Transactional
	public Onboarding save(Long accountId, AgeRange ageRange, MenstrualStatus menstrualStatus) {
		requireAccountExists(accountId);
		Onboarding onboarding = onboardingRepository.findByAccountId(accountId)
				.orElseGet(() -> onboardingRepository.save(Onboarding.createFor(accountId)));
		onboarding.submitAnswers(ageRange, menstrualStatus, LocalDateTime.now());
		return onboarding;
	}

	private void requireAccountExists(Long accountId) {
		accountRepository.findById(accountId)
				.orElseThrow(() -> new NotFoundException("계정을 찾을 수 없습니다."));
	}
}
