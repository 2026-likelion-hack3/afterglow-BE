package com.afterglow.domain.episode.analysis.domain;

/** 후보별 근거 강도와 점수 환산(강=3/중=2/약=1) — 기능명세서 2.4, 2026-08-13 확정. */
public enum EvidenceStrength {
	STRONG(3),
	MEDIUM(2),
	WEAK(1);

	private final int score;

	EvidenceStrength(int score) {
		this.score = score;
	}

	public int score() {
		return score;
	}
}
