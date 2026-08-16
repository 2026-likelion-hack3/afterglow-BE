package com.afterglow.domain.onboarding.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.domain.onboarding.application.OnboardingService;
import com.afterglow.domain.onboarding.domain.Onboarding;
import com.afterglow.global.security.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class OnboardingController {

	private final OnboardingService onboardingService;

	@GetMapping
	public ResponseEntity<OnboardingResponse> getOnboarding(@AuthenticationPrincipal Long accountId) {
		Onboarding onboarding = onboardingService.get(accountId);
		if (onboarding == null) {
			return ResponseEntity.ok(new OnboardingResponse(null, null, null));
		}
		return ResponseEntity.ok(toResponse(onboarding));
	}

	@PutMapping
	public ResponseEntity<OnboardingResponse> submitOnboarding(
			@AuthenticationPrincipal Long accountId, @Valid @RequestBody OnboardingRequest request) {
		Onboarding onboarding = onboardingService.save(accountId, request.ageRange(), request.menstrualStatus());
		return ResponseEntity.ok(toResponse(onboarding));
	}

	private OnboardingResponse toResponse(Onboarding onboarding) {
		return new OnboardingResponse(
				onboarding.getAgeRange(), onboarding.getMenstrualStatus(), onboarding.getOnboardingCompletedAt());
	}
}
