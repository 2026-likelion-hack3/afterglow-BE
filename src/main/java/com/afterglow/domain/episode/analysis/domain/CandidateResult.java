package com.afterglow.domain.episode.analysis.domain;

/** 7일 게이트를 통과해 근거 강도까지 계산된 후보 하나. */
public record CandidateResult(CandidateType type, EvidenceStrength strength, Evidence evidence) {

	public int score() {
		return strength.score();
	}
}
