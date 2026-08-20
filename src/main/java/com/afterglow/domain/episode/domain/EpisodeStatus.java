package com.afterglow.domain.episode.domain;

public enum EpisodeStatus {
	SYMPTOM_SELECTED,
	INTAKE_COMPLETED,
	/** 통합 분석 완료(2.4) — 확신 수준 미달("판단하기 이릅니다")도 정상 완료라 이 상태로 전이한다. */
	ANALYZED
}
