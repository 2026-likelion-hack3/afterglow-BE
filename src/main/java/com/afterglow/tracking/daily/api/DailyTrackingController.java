package com.afterglow.tracking.daily.api;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.common.security.OpenApiConfig;
import com.afterglow.tracking.daily.application.DailyTrackingService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tracking/daily")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class DailyTrackingController {

	private final DailyTrackingService dailyTrackingService;

	@PostMapping
	public ResponseEntity<Void> createOrUpdate(
		@AuthenticationPrincipal Long accountId,
		@Valid @RequestBody DailyTrackingRequest request) {

		dailyTrackingService.createOrUpdate(
			accountId,
			request.recordedDate(),
			request.sleepLevel(),
			request.conditionLevel(),
			request.latitude(),
			request.longitude()
		);

		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<DailyTrackingResponse> getDailyTracking(
		@AuthenticationPrincipal Long accountId) {

		DailyTrackingResponse response = dailyTrackingService.getDailyTracking(
			accountId,
			LocalDate.now()
		);

		if (response == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(response);
	}
}
