package com.afterglow.episode.intake.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.episode.intake.application.EpisodeService;
import com.afterglow.episode.intake.domain.Intake;
import com.afterglow.episode.intake.domain.Symptom;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/episodes")
@RequiredArgsConstructor
public class EpisodeController {

	private final EpisodeService episodeService;

	@PostMapping
	public ResponseEntity<EpisodeResponse> createEpisode(
			@AuthenticationPrincipal Long accountId, @Valid @RequestBody SymptomRequest request) {
		Symptom symptom = Symptom.create(request.angle(), request.radius(), request.primarySymptom(), request.severity());
		Long episodeId = episodeService.createEpisode(accountId, symptom);
		return ResponseEntity.ok(new EpisodeResponse(episodeId));
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
