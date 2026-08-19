package com.afterglow.domain.episode.analysis.domain;

/** explanation이 실제로 AI에서 나왔는지, 미설정/장애로 결정적 fallback을 썼는지 — 테스트/디버깅용. */
public enum ExplanationSource {
	AI,
	FALLBACK
}
