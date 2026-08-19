package com.afterglow.domain.episode.analysis.api;

import com.afterglow.domain.episode.analysis.domain.CandidateType;
import com.afterglow.domain.episode.analysis.domain.Evidence;
import com.afterglow.domain.episode.card.domain.ResultCard;
import com.afterglow.domain.episode.card.domain.ResultCardType;

public record ResultCardResponse(ResultCardType type, CandidateType causeType, Evidence evidence, Long coverageDays) {

	public static ResultCardResponse from(ResultCard card) {
		return new ResultCardResponse(card.type(), card.causeType(), card.evidence(), card.coverageDays());
	}
}
