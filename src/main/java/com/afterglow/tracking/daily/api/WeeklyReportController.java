package com.afterglow.tracking.daily.api;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.global.security.OpenApiConfig;
import com.afterglow.tracking.daily.application.WeeklyReportService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tracking/weekly-report")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class WeeklyReportController {

	private final WeeklyReportService weeklyReportService;

	@GetMapping
	public ResponseEntity<WeeklyReportResponse> getWeeklyReport(
		@AuthenticationPrincipal Long accountId,
		@RequestParam LocalDate from,
		@RequestParam LocalDate to) {

		WeeklyReportResponse response =
			weeklyReportService.getWeeklyReport(accountId, from, to);

		return ResponseEntity.ok(response);
	}
}
