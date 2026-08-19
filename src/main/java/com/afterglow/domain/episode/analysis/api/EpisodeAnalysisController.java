package com.afterglow.domain.episode.analysis.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.domain.episode.analysis.application.EpisodeAnalysisService;
import com.afterglow.domain.episode.card.domain.ResultCardResult;
import com.afterglow.global.security.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

/**
 * 기능명세서 2.4/3.1(Manyfast F-ZSPZHH/F-HGUJDZ) — 명세에 URL/request/response 계약이 명시돼 있지
 * 않아, 기존 {@code /api/episodes/{episodeId}/...} convention(예: {@code RoutineController})을 그대로
 * 따라 마감에 필요한 최소 API만 구현했다.
 */
@RestController
@RequestMapping("/api/episodes/{episodeId}/analysis")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class EpisodeAnalysisController {

	private final EpisodeAnalysisService episodeAnalysisService;

	/** 이미 분석된 Episode면 재실행하지 않고 기존 결과를 그대로 반환한다(idempotent). */
	@PostMapping
	public ResponseEntity<EpisodeAnalysisResponse> analyze(
			@AuthenticationPrincipal Long accountId, @PathVariable Long episodeId) {
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);
		return ResponseEntity.ok(EpisodeAnalysisResponse.from(result));
	}

	@GetMapping
	public ResponseEntity<EpisodeAnalysisResponse> getResult(
			@AuthenticationPrincipal Long accountId, @PathVariable Long episodeId) {
		ResultCardResult result = episodeAnalysisService.getResult(accountId, episodeId);
		return ResponseEntity.ok(EpisodeAnalysisResponse.from(result));
	}
}
