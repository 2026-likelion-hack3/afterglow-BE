package com.afterglow.domain.episode.routine.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 기획 확정(2026-08-19): "3일 루틴에서 사용중단 대상으로 지정하면 중단된 것", "중단됐던 제품이 이후
 * Routine에 다시 포함되면 재개된 것, 재개일은 그 Routine의 시작일". Vanity를 전혀 참조하지 않는 순수 함수
 * 테스트라 이 클래스 자체가 "Analysis 기준일에 손대지 않는다"는 걸 구조적으로 증명한다 — 이 테스트에는
 * Vanity/Analysis 관련 의존성이 아예 없다.
 */
class RoutineProductLifecycleTest {

	private static final long PRODUCT_ID = 100L;

	private final RoutineProductLifecycle lifecycle = new RoutineProductLifecycle();

	@Test
	void 중단_대상으로_지정된_제품이면_DISCONTINUE_사건으로_확인된다() {
		Routine routine = routineWithDiscontinue(1L, LocalDate.of(2026, 8, 1));

		List<ProductUsageEvent> events = lifecycle.historyOf(List.of(routine), PRODUCT_ID);

		assertThat(events).hasSize(1);
		assertThat(events.get(0).usage()).isEqualTo(RoutineItemUsage.DISCONTINUE);
		assertThat(events.get(0).date()).isEqualTo(LocalDate.of(2026, 8, 1));
		assertThat(events.get(0).resumed()).isFalse();
	}

	@Test
	void 중단됐던_제품이_이후_Routine에_CONTINUE로_포함되면_재개로_판단된다() {
		Routine discontinuedIn = routineWithDiscontinue(1L, LocalDate.of(2026, 8, 1));
		Routine resumedIn = routineWithContinue(2L, LocalDate.of(2026, 8, 10));

		List<ProductUsageEvent> events = lifecycle.historyOf(List.of(discontinuedIn, resumedIn), PRODUCT_ID);

		assertThat(events).hasSize(2);
		assertThat(events.get(1).usage()).isEqualTo(RoutineItemUsage.CONTINUE);
		assertThat(events.get(1).resumed()).isTrue();
	}

	@Test
	void 재개일은_다시_포함된_Routine의_시작일이다() {
		Routine discontinuedIn = routineWithDiscontinue(1L, LocalDate.of(2026, 8, 1));
		LocalDate laterStartDate = LocalDate.of(2026, 8, 15);
		Routine resumedIn = routineWithContinue(2L, laterStartDate);

		List<ProductUsageEvent> events = lifecycle.historyOf(List.of(discontinuedIn, resumedIn), PRODUCT_ID);

		assertThat(events.get(1).date()).isEqualTo(laterStartDate);
	}

	@Test
	void 처음부터_CONTINUE면_재개가_아니다() {
		Routine routine = routineWithContinue(1L, LocalDate.of(2026, 8, 1));

		List<ProductUsageEvent> events = lifecycle.historyOf(List.of(routine), PRODUCT_ID);

		assertThat(events.get(0).resumed()).isFalse();
	}

	@Test
	void 재개_후_다시_중단되면_그건_재개가_아니다() {
		Routine discontinuedIn = routineWithDiscontinue(1L, LocalDate.of(2026, 8, 1));
		Routine resumedIn = routineWithContinue(2L, LocalDate.of(2026, 8, 10));
		Routine discontinuedAgainIn = routineWithDiscontinue(3L, LocalDate.of(2026, 8, 20));

		List<ProductUsageEvent> events = lifecycle.historyOf(List.of(discontinuedIn, resumedIn, discontinuedAgainIn), PRODUCT_ID);

		assertThat(events).hasSize(3);
		assertThat(events.get(2).usage()).isEqualTo(RoutineItemUsage.DISCONTINUE);
		assertThat(events.get(2).resumed()).isFalse();
	}

	@Test
	void 재개가_원래_중단_이력을_바꾸지_않는다() {
		Routine discontinuedIn = routineWithDiscontinue(1L, LocalDate.of(2026, 8, 1));
		Routine resumedIn = routineWithContinue(2L, LocalDate.of(2026, 8, 10));

		lifecycle.historyOf(List.of(discontinuedIn, resumedIn), PRODUCT_ID);

		// historyOf 호출은 순수 조회다 — Routine 자체의 상태(중단 대상 목록)는 그대로 유지된다.
		assertThat(discontinuedIn.discontinuedProductIds()).containsExactly(PRODUCT_ID);
	}

	@Test
	void 이_제품과_무관한_Routine은_이력에_포함되지_않는다() {
		Routine unrelated = routineWithContinue(1L, LocalDate.of(2026, 8, 1), 777L);

		List<ProductUsageEvent> events = lifecycle.historyOf(List.of(unrelated), PRODUCT_ID);

		assertThat(events).isEmpty();
	}

	private Routine routineWithDiscontinue(Long episodeId, LocalDate startDate) {
		return Routine.create(episodeId, 1L, startDate, List.of(RoutineItem.discontinueItem(PRODUCT_ID)));
	}

	private Routine routineWithContinue(Long episodeId, LocalDate startDate) {
		return routineWithContinue(episodeId, startDate, PRODUCT_ID);
	}

	private Routine routineWithContinue(Long episodeId, LocalDate startDate, Long productId) {
		return Routine.create(episodeId, 1L, startDate, List.of(RoutineItem.continueItem(1, RoutineTimeSlot.MORNING, productId)));
	}
}
