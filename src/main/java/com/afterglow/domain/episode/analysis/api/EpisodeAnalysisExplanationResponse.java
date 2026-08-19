package com.afterglow.domain.episode.analysis.api;

import java.util.List;

import com.afterglow.domain.episode.analysis.domain.AnalysisExplanationOutcome;
import com.afterglow.domain.episode.analysis.domain.ExplanationSource;

/** {@code source}는 프론트 표시용이라기보다 장애/fallback 여부를 테스트·디버깅에서 확인하기 위한 값이다. */
public record EpisodeAnalysisExplanationResponse(
		String headline,
		String summary,
		List<String> evidenceNotes,
		String nextAction,
		ExplanationSource source
) {

	public static EpisodeAnalysisExplanationResponse from(AnalysisExplanationOutcome outcome) {
		return new EpisodeAnalysisExplanationResponse(
				outcome.explanation().headline(),
				outcome.explanation().summary(),
				outcome.explanation().evidenceNotes(),
				outcome.explanation().nextAction(),
				outcome.source()
		);
	}
}
