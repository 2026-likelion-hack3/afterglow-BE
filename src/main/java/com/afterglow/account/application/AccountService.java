package com.afterglow.account.application;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.account.domain.Account;
import com.afterglow.account.domain.AccountRepository;
import com.afterglow.account.domain.EmailSender;
import com.afterglow.account.domain.EmailVerification;
import com.afterglow.account.domain.EmailVerificationRepository;
import com.afterglow.account.domain.VerificationPurpose;
import com.afterglow.common.exception.AfterglowException;
import com.afterglow.common.exception.ErrorCode;
import com.afterglow.common.exception.NotFoundException;
import com.afterglow.common.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

	private static final int VERIFICATION_CODE_LENGTH = 6;
	private static final long VERIFICATION_CODE_TTL_MINUTES = 5;

	private final AccountRepository accountRepository;
	private final EmailVerificationRepository emailVerificationRepository;
	private final EmailSender emailSender;
	private final JwtTokenProvider jwtTokenProvider;
	private final SecureRandom secureRandom = new SecureRandom();

	/** 기기 최초 실행 시 호출한다. 이 시점부터 이메일 없이도 모든 도메인 기능을 이 계정으로 이용할 수 있다. */
	public String createAnonymousAccount() {
		Account account = accountRepository.save(Account.createAnonymous());
		return jwtTokenProvider.createToken(account.getId());
	}

	public void requestSignupVerificationCode(Long accountId, String email) {
		getAccount(accountId);
		if (accountRepository.existsByEmailAndEmailVerifiedAtIsNotNull(email)) {
			throw new AfterglowException(ErrorCode.EMAIL_ALREADY_REGISTERED);
		}
		issueCode(email, VerificationPurpose.SIGNUP);
	}

	/** 인증 성공 시 현재(익명) 계정에 이메일을 연결한다 — 새 계정을 만들지 않으므로 기존 기기 기록이 그대로 유지된다. */
	public String verifySignupCode(Long accountId, String email, String code) {
		Account account = getAccount(accountId);
		if (accountRepository.existsByEmailAndEmailVerifiedAtIsNotNull(email)) {
			throw new AfterglowException(ErrorCode.EMAIL_ALREADY_REGISTERED);
		}
		consumeValidCode(email, VerificationPurpose.SIGNUP, code);
		account.verifyEmail(email, LocalDateTime.now());
		return jwtTokenProvider.createToken(account.getId());
	}

	public void requestLoginVerificationCode(String email) {
		accountRepository.findByEmailAndEmailVerifiedAtIsNotNull(email)
				.orElseThrow(() -> new NotFoundException("등록된 계정을 찾을 수 없습니다."));
		issueCode(email, VerificationPurpose.LOGIN);
	}

	/** 로그인은 새 기기의 익명 계정을 버리고, 이메일이 연결된 기존 계정의 토큰을 새로 발급하는 방식으로 처리한다. */
	public String verifyLoginCode(String email, String code) {
		Account account = accountRepository.findByEmailAndEmailVerifiedAtIsNotNull(email)
				.orElseThrow(() -> new NotFoundException("등록된 계정을 찾을 수 없습니다."));
		consumeValidCode(email, VerificationPurpose.LOGIN, code);
		return jwtTokenProvider.createToken(account.getId());
	}

	/**
	 * 계정만 삭제한다. 다른 도메인이 실제로 accountId를 참조하는 데이터를 쌓기 시작하면
	 * 그 시점에 연관 데이터 정리 방식(이벤트 기반 정리 등)을 다시 정한다 — 아직은 참조하는 데이터가 없다.
	 */
	public void deleteAccount(Long accountId) {
		Account account = getAccount(accountId);
		accountRepository.delete(account);
	}

	private Account getAccount(Long accountId) {
		return accountRepository.findById(accountId)
				.orElseThrow(() -> new NotFoundException("계정을 찾을 수 없습니다."));
	}

	private void issueCode(String email, VerificationPurpose purpose) {
		String code = generateCode();
		emailVerificationRepository.save(
				EmailVerification.create(email, code, purpose, LocalDateTime.now().plusMinutes(VERIFICATION_CODE_TTL_MINUTES)));
		emailSender.sendVerificationCode(email, code);
	}

	private void consumeValidCode(String email, VerificationPurpose purpose, String code) {
		EmailVerification verification = emailVerificationRepository
				.findTopByEmailAndPurposeAndCodeOrderByCreatedAtDesc(email, purpose, code)
				.orElseThrow(() -> new AfterglowException(ErrorCode.INVALID_VERIFICATION_CODE));
		if (verification.isConsumed()) {
			throw new AfterglowException(ErrorCode.INVALID_VERIFICATION_CODE);
		}
		if (verification.isExpired(LocalDateTime.now())) {
			throw new AfterglowException(ErrorCode.VERIFICATION_CODE_EXPIRED);
		}
		verification.consume();
	}

	private String generateCode() {
		int bound = (int) Math.pow(10, VERIFICATION_CODE_LENGTH);
		return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", secureRandom.nextInt(bound));
	}
}
