package com.afterglow.domain.episode.checkin.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.domain.episode.checkin.application.CheckInService;
import com.afterglow.domain.episode.checkin.domain.CheckIn;
import com.afterglow.global.security.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/episodes/{episodeId}/check-ins")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CheckInController {

	private final CheckInService checkInService;

	@PutMapping("/{date}")
	public ResponseEntity<CheckInResponse> record(
			@AuthenticationPrincipal Long accountId, @PathVariable Long episodeId,
			@PathVariable LocalDate date, @Valid @RequestBody CheckInRequest request) {
		CheckIn checkIn = checkInService.record(accountId, episodeId, date, request.status());
		return ResponseEntity.ok(toResponse(checkIn));
	}

	@GetMapping
	public ResponseEntity<List<CheckInResponse>> getAll(
			@AuthenticationPrincipal Long accountId, @PathVariable Long episodeId) {
		List<CheckInResponse> responses = checkInService.getAll(accountId, episodeId).stream()
				.map(this::toResponse)
				.toList();
		return ResponseEntity.ok(responses);
	}

	private CheckInResponse toResponse(CheckIn checkIn) {
		return new CheckInResponse(checkIn.getCheckInDate(), checkIn.getStatus());
	}
}
