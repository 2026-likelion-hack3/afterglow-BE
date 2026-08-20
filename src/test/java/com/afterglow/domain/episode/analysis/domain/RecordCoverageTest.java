package com.afterglow.domain.episode.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecordCoverageTest {

	@Test
	void record_건수가_6이면_게이트를_통과하지_못한다() {
		RecordCoverage coverage = new RecordCoverage(6);

		assertThat(coverage.coverageDays()).isEqualTo(6);
		assertThat(coverage.meetsSevenDayGate()).isFalse();
	}

	@Test
	void record_건수가_정확히_7이면_게이트를_통과한다() {
		RecordCoverage coverage = new RecordCoverage(7);

		assertThat(coverage.coverageDays()).isEqualTo(7);
		assertThat(coverage.meetsSevenDayGate()).isTrue();
	}

	@Test
	void record_건수가_7보다_많으면_게이트를_통과한다() {
		RecordCoverage coverage = new RecordCoverage(8);

		assertThat(coverage.coverageDays()).isEqualTo(8);
		assertThat(coverage.meetsSevenDayGate()).isTrue();
	}
}
