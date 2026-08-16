package com.afterglow.domain.episode.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class RecordCoverageTest {

	private static final LocalDate ANALYSIS_DATE = LocalDate.of(2026, 8, 16);

	@Test
	void 최초_기록일이_6_calendar_days_전이면_게이트를_통과하지_못한다() {
		RecordCoverage coverage = new RecordCoverage(LocalDate.of(2026, 8, 11));

		assertThat(coverage.coverageDays(ANALYSIS_DATE)).isEqualTo(6);
		assertThat(coverage.meetsSevenDayGate(ANALYSIS_DATE)).isFalse();
	}

	@Test
	void 최초_기록일이_정확히_7_calendar_days_전이면_게이트를_통과한다() {
		RecordCoverage coverage = new RecordCoverage(LocalDate.of(2026, 8, 10));

		assertThat(coverage.coverageDays(ANALYSIS_DATE)).isEqualTo(7);
		assertThat(coverage.meetsSevenDayGate(ANALYSIS_DATE)).isTrue();
	}

	@Test
	void 최초_기록일이_7_calendar_days보다_더_이전이면_게이트를_통과한다() {
		RecordCoverage coverage = new RecordCoverage(LocalDate.of(2026, 8, 9));

		assertThat(coverage.coverageDays(ANALYSIS_DATE)).isEqualTo(8);
		assertThat(coverage.meetsSevenDayGate(ANALYSIS_DATE)).isTrue();
	}
}
