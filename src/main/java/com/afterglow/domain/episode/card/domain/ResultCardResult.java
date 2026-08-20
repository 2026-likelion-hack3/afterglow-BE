package com.afterglow.domain.episode.card.domain;

import java.util.List;

import com.afterglow.domain.episode.analysis.domain.Confidence;

/**
 * {@link ResultCardAssembler}의 산출물 — 항상 카드 3장을 담는다. {@link AnalysisResult}의 hold semantics를
 * 그대로 따른다: HOLD면 {@code holdReason}만 채워지고 {@code confidence}는 null, HOLD가 아니면
 * {@code confidence}만 채워지고 {@code holdReason}은 null이다(compact constructor가 강제).
 */
public record ResultCardResult(
		boolean hold,
		ResultCardHoldReason holdReason,
		Confidence confidence,
		List<ResultCard> cards
) {
	public ResultCardResult {
		cards = List.copyOf(cards);
		if (hold && (holdReason == null || confidence != null)) {
			throw new IllegalStateException("HOLD 결과는 holdReason만 있고 confidence는 없어야 한다.");
		}
		if (!hold && (holdReason != null || confidence == null)) {
			throw new IllegalStateException("HOLD가 아닌 결과는 confidence만 있고 holdReason은 없어야 한다.");
		}
	}

	public static ResultCardResult hold(ResultCardHoldReason holdReason, List<ResultCard> cards) {
		return new ResultCardResult(true, holdReason, null, cards);
	}

	public static ResultCardResult determined(Confidence confidence, List<ResultCard> cards) {
		return new ResultCardResult(false, null, confidence, cards);
	}
}
