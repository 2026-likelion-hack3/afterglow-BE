package com.afterglow.domain.episode.analysis.domain;

import java.time.LocalDate;
import java.util.Set;

/**
 * 제품/제품 조합 candidate 하나의 관측 window와, 그 안에서 실제 CheckIn이 존재한 날짜 수(coverageDays,
 * 2026-08-20 확정 — calendar-day 길이가 아니라 실제 기록이 존재한 날짜 수)를 계산하는 순수 함수.
 * Repository/Vanity/CheckIn 구체 타입에 의존하지 않는다 — 호출자가 이미 계산해 둔 CheckIn 날짜 집합만 받는다.
 *
 * <p>window 끝 = 다음 중 가장 이른 날짜: 기준일+13일(14일 창), 분석 기준일(오늘), Routine이 이 제품을
 * 중단시킨 날짜(있다면) — {@link com.afterglow.domain.episode.routine.domain.RoutineProductLifecycle}가
 * 알려주는 중단일을 그대로 전달받는다.
 */
public record ProductObservationWindow(LocalDate start, LocalDate end) {

	private static final long WINDOW_DURATION_DAYS = 14;

	public static ProductObservationWindow of(LocalDate referenceDate, LocalDate analysisDate, LocalDate discontinuationDate) {
		LocalDate windowEnd = referenceDate.plusDays(WINDOW_DURATION_DAYS - 1);
		if (analysisDate.isBefore(windowEnd)) {
			windowEnd = analysisDate;
		}
		if (discontinuationDate != null && discontinuationDate.isBefore(windowEnd)) {
			windowEnd = discontinuationDate;
		}
		return new ProductObservationWindow(referenceDate, windowEnd);
	}

	/** window 안에 있는 {@code checkInDates}의 개수 — window가 뒤집혀 있으면(끝이 시작보다 이전) 0. */
	public long coverageDays(Set<LocalDate> checkInDates) {
		if (end.isBefore(start)) {
			return 0;
		}
		return checkInDates.stream().filter(date -> !date.isBefore(start) && !date.isAfter(end)).count();
	}
}
