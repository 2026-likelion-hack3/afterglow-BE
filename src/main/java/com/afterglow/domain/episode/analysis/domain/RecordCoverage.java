package com.afterglow.domain.episode.analysis.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 후보 데이터의 최초 기록일. "기록 7일" 게이트는 record 건수가 아니라 최초 기록일부터 분석 기준일까지의
 * calendar-day 커버리지로 계산한다(예: 8/10~8/16 = 7 calendar days). 관측 건수(예: 수면/날씨의
 * observations 3~5회) 기준과는 별개의 규칙이다.
 */
public record RecordCoverage(LocalDate firstRecordDate) {

	public long coverageDays(LocalDate analysisDate) {
		return ChronoUnit.DAYS.between(firstRecordDate, analysisDate) + 1;
	}

	public boolean meetsSevenDayGate(LocalDate analysisDate) {
		return coverageDays(analysisDate) >= 7;
	}
}
