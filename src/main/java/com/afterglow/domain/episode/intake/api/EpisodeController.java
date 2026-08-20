package com.afterglow.domain.episode.intake.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.global.security.OpenApiConfig;
import com.afterglow.domain.episode.application.EpisodeSummaryService;
import com.afterglow.domain.episode.intake.application.EpisodeService;
import com.afterglow.domain.episode.domain.Intake;
import com.afterglow.domain.episode.domain.Symptom;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/episodes")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class EpisodeController {

	private final EpisodeService episodeService;
	private final EpisodeSummaryService episodeSummaryService;

	@PostMapping
	public ResponseEntity<EpisodeResponse> createEpisode(
			@AuthenticationPrincipal Long accountId, @Valid @RequestBody SymptomRequest request) {
		Symptom symptom = Symptom.create(request.angle(), request.radius(), request.primarySymptom(), request.severity());
		Long episodeId = episodeService.createEpisode(accountId, symptom);
		return ResponseEntity.ok(new EpisodeResponse(episodeId));
	}

	/** Figma E1(기록 목록) — 본인 계정의 Episode를 최신순으로 반환한다. */
	@GetMapping
	public ResponseEntity<List<EpisodeSummaryResponse>> getEpisodes(@AuthenticationPrincipal Long accountId) {
		return ResponseEntity.ok(episodeSummaryService.getEpisodes(accountId));
	}

	@PostMapping("/{episodeId}/intake")
	public ResponseEntity<Void> submitIntake(
			@AuthenticationPrincipal Long accountId, @PathVariable Long episodeId,
			@Valid @RequestBody IntakeRequest request) {
		Intake intake = Intake.create(request.onsetPeriod(), request.recentNewProductName(), request.notes());
		episodeService.submitIntake(accountId, episodeId, intake, request.bodyParts());
		return ResponseEntity.noContent().build();
	}
}
