package com.afterglow.domain.episode.analysis.api;

import java.util.List;

import com.afterglow.domain.episode.analysis.domain.Confidence;
import com.afterglow.domain.episode.card.domain.ResultCardHoldReason;
import com.afterglow.domain.episode.card.domain.ResultCardResult;

public record EpisodeAnalysisResponse(
		boolean hold,
		ResultCardHoldReason holdReason,
		Confidence confidence,
		List<ResultCardResponse> cards
) {

	public static EpisodeAnalysisResponse from(ResultCardResult result) {
		return new EpisodeAnalysisResponse(
				result.hold(),
				result.holdReason(),
				result.confidence(),
				result.cards().stream().map(ResultCardResponse::from).toList()
		);
	}
}
