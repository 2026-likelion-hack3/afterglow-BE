package com.afterglow.account.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.account.application.AccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	@PostMapping("/anonymous")
	public ResponseEntity<TokenResponse> createAnonymousAccount() {
		String token = accountService.createAnonymousAccount();
		return ResponseEntity.ok(new TokenResponse(token));
	}

	@PostMapping("/me/email/verification-codes")
	public ResponseEntity<Void> requestSignupVerificationCode(
			@AuthenticationPrincipal Long accountId, @Valid @RequestBody EmailRequest request) {
		accountService.requestSignupVerificationCode(accountId, request.email());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/me/email/verification")
	public ResponseEntity<TokenResponse> verifySignup(
			@AuthenticationPrincipal Long accountId, @Valid @RequestBody EmailVerifyRequest request) {
		String token = accountService.verifySignupCode(accountId, request.email(), request.code());
		return ResponseEntity.ok(new TokenResponse(token));
	}

	@PostMapping("/login/verification-codes")
	public ResponseEntity<Void> requestLoginVerificationCode(@Valid @RequestBody EmailRequest request) {
		accountService.requestLoginVerificationCode(request.email());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/login/verification")
	public ResponseEntity<TokenResponse> verifyLogin(@Valid @RequestBody EmailVerifyRequest request) {
		String token = accountService.verifyLoginCode(request.email(), request.code());
		return ResponseEntity.ok(new TokenResponse(token));
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Long accountId) {
		accountService.deleteAccount(accountId);
		return ResponseEntity.noContent().build();
	}
}
