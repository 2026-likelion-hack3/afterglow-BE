package com.afterglow.domain.episode.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ProductObservationWindowTest {

	private static final LocalDate REFERENCE = LocalDate.of(2026, 8, 1);
	private static final LocalDate ANALYSIS_DATE = LocalDate.of(2026, 8, 20);

	@Test
	void coverageDays는_calendar_기간이_아니라_실제_CheckIn이_존재한_날짜_수다() {
		ProductObservationWindow window = ProductObservationWindow.of(REFERENCE, ANALYSIS_DATE, null);
		// window: 2026-08-01 ~ 2026-08-14(14일), 그중 실제 CheckIn은 2일뿐.
		Set<LocalDate> checkInDates = Set.of(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10));

		assertThat(window.coverageDays(checkInDates)).isEqualTo(2);
	}

	@Test
	void window_밖의_CheckIn은_coverage에_포함되지_않는다() {
		ProductObservationWindow window = ProductObservationWindow.of(REFERENCE, ANALYSIS_DATE, null);
		Set<LocalDate> checkInDates = Set.of(REFERENCE.minusDays(1), REFERENCE.plusDays(20));

		assertThat(window.coverageDays(checkInDates)).isZero();
	}

	@Test
	void window_끝은_기준일_13일_후_분석일_discontinuation_중_가장_이른_날짜다_기본은_14일_창() {
		ProductObservationWindow window = ProductObservationWindow.of(REFERENCE, ANALYSIS_DATE, null);

		assertThat(window.end()).isEqualTo(REFERENCE.plusDays(13));
	}

	@Test
	void 분석일이_14일_창보다_먼저_끝나면_분석일이_window_끝이다() {
		LocalDate earlyAnalysisDate = REFERENCE.plusDays(5);
		ProductObservationWindow window = ProductObservationWindow.of(REFERENCE, earlyAnalysisDate, null);

		assertThat(window.end()).isEqualTo(earlyAnalysisDate);
	}

	@Test
	void Routine_중단일이_있으면_그보다_늦은_날짜는_window에서_제외된다() {
		LocalDate discontinuedAt = REFERENCE.plusDays(4);
		ProductObservationWindow window = ProductObservationWindow.of(REFERENCE, ANALYSIS_DATE, discontinuedAt);

		assertThat(window.end()).isEqualTo(discontinuedAt);
		Set<LocalDate> checkInDates = Set.of(REFERENCE.plusDays(2), REFERENCE.plusDays(10));
		assertThat(window.coverageDays(checkInDates)).isEqualTo(1);
	}
}
