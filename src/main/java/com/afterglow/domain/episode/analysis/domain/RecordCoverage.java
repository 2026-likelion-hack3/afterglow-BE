package com.afterglow.domain.episode.analysis.domain;

/**
 * SLEEP/WEATHER 후보의 "기록 7일" 게이트 — record 건수(관측 가능한 날짜 수) 기준이다(2026-08-20 정정).
 * 이전에는 최초 기록일부터 분석 기준일까지의 calendar-day span으로 계산했으나, "실제로 분석 가능한
 * observation이 존재하는 날짜 수"와 calendar span은 다른 값이다(예: 8/1·8/7 두 날짜만 기록돼 있으면
 * calendar span은 7이지만 실제 관측은 2건뿐이다) — 그래서 record 건수 자체를 coverage로 쓴다. 임계값
 * 7은 바뀌지 않았다 — "무엇과 비교하는지"만 바뀌었다.
 */
public record RecordCoverage(long coverageDays) {

	public boolean meetsSevenDayGate() {
		return coverageDays >= 7;
	}
}
