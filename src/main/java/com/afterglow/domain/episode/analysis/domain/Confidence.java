package com.afterglow.domain.episode.analysis.domain;

/** HOLD가 아닌 경우의 확신 단계. HOLD 자체는 {@link AnalysisResult#hold()}로 별도 표현한다. */
public enum Confidence {
	HIGH,
	NORMAL
}
