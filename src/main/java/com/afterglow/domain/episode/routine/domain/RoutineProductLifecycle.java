package com.afterglow.domain.episode.routine.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 계정의 Routine 이력(시작일 오름차순)에서 특정 제품의 중단/재개 사건을 도출하는 순수 함수(Repository/
 * Spring 의존 없음, {@link com.afterglow.domain.episode.checkin.domain.Day3JudgmentEngine}과 동일한 스타일).
 *
 * <p><b>Vanity {@code Product}는 여기서도 절대 건드리지 않는다</b> — discontinuedAt/resumedAt을 저장하거나
 * Vanity를 호출하지 않고, 오직 이미 저장된 Routine/RoutineItem만 읽어 사건을 재구성한다. 원래 제품 사용
 * 기준일({@code ProductCandidateInput.usageStartDate})은 이 클래스가 알지도, 바꾸지도 않는다.
 *
 * <p><b>중요(2026-08-19 기획 확정)</b>: 앱이(Routine을 통해) 중단시켰다가 재개된 제품은 원래 기준일을
 * 갱신하지 않는다. 이 클래스가 만들어내는 {@link ProductUsageEvent#resumed()}는 "재개가 일어났다는 사실"만
 * 알려줄 뿐, Analysis의 기준일 계산 로직은 이 정보를 보고도 기준일을 리셋하지 않아야 한다 — 그 결정은 이
 * 클래스가 아니라 향후 Analysis 통합 지점의 책임이다(아직 미구현, 보고서의 BLOCKED/contract 참고).
 */
public final class RoutineProductLifecycle {

	/** {@code routines}는 반드시 시작일(동점이면 id) 오름차순이어야 한다({@link RoutineRepository#findByAccountIdOrderByStartDateAscIdAsc}). */
	public List<ProductUsageEvent> historyOf(List<Routine> routines, Long productId) {
		List<ProductUsageEvent> events = new ArrayList<>();
		RoutineItemUsage previous = null;

		for (Routine routine : routines) {
			RoutineItemUsage current = usageIn(routine, productId);
			if (current == null) {
				continue;
			}
			boolean resumed = current == RoutineItemUsage.CONTINUE && previous == RoutineItemUsage.DISCONTINUE;
			events.add(new ProductUsageEvent(current, routine.getStartDate(), resumed));
			previous = current;
		}

		return events;
	}

	/** 이 Routine에서 productId가 어떻게 다뤄졌는지 — DISCONTINUE 우선, 아니면 CONTINUE 여부, 둘 다 아니면 null(무관). */
	private RoutineItemUsage usageIn(Routine routine, Long productId) {
		if (routine.discontinuedProductIds().contains(productId)) {
			return RoutineItemUsage.DISCONTINUE;
		}
		boolean continued = IntStream.rangeClosed(1, Routine.DURATION_DAYS)
				.boxed()
				.flatMap(dayNumber -> routine.continueItemsOf(dayNumber).stream())
				.anyMatch(item -> item.getProductId().equals(productId));
		return continued ? RoutineItemUsage.CONTINUE : null;
	}
}
