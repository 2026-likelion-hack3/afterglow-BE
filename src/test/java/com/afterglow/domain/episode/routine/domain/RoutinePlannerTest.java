package com.afterglow.domain.episode.routine.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

class RoutinePlannerTest {

	private final RoutinePlanner planner = new RoutinePlanner();
	private final LocalDate startDate = LocalDate.of(2026, 8, 20);

	@Test
	void 정상_입력이면_Routine이_생성된다() {
		RoutineCreateInput input = new RoutineCreateInput(List.of(
				new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, 100L),
				new RoutineItemInput(RoutineItemUsage.DISCONTINUE, null, null, 999L)
		));

		Routine routine = planner.plan(1L, 10L, startDate, input);

		assertThat(routine.getEpisodeId()).isEqualTo(1L);
		assertThat(routine.getAccountId()).isEqualTo(10L);
		assertThat(routine.getStartDate()).isEqualTo(startDate);
		assertThat(routine.getStatus()).isEqualTo(RoutineStatus.ACTIVE);
	}

	@Test
	void Day1_Day2_Day3_날짜가_시작일_기준으로_정확히_계산된다() {
		Routine routine = planner.plan(1L, 10L, startDate, new RoutineCreateInput(List.of()));

		assertThat(routine.dateOf(1)).isEqualTo(LocalDate.of(2026, 8, 20));
		assertThat(routine.dateOf(2)).isEqualTo(LocalDate.of(2026, 8, 21));
		assertThat(routine.dateOf(3)).isEqualTo(LocalDate.of(2026, 8, 22));
	}

	@Test
	void 범위를_벗어난_dayNumber로_날짜를_조회하면_예외가_발생한다() {
		Routine routine = planner.plan(1L, 10L, startDate, new RoutineCreateInput(List.of()));

		assertThatThrownBy(() -> routine.dateOf(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> routine.dateOf(4)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void morning_evening_구성이_day별로_올바르게_묶인다() {
		RoutineCreateInput input = new RoutineCreateInput(List.of(
				new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, 100L),
				new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.EVENING, 200L),
				new RoutineItemInput(RoutineItemUsage.CONTINUE, 2, RoutineTimeSlot.MORNING, 100L)
		));

		Routine routine = planner.plan(1L, 10L, startDate, input);

		assertThat(routine.continueItemsOf(1)).hasSize(2)
				.extracting(RoutineItem::getTimeSlot, RoutineItem::getProductId)
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple(RoutineTimeSlot.MORNING, 100L),
						org.assertj.core.groups.Tuple.tuple(RoutineTimeSlot.EVENING, 200L));
		assertThat(routine.continueItemsOf(2)).hasSize(1);
		assertThat(routine.continueItemsOf(3)).isEmpty();
	}

	@Test
	void 중단_대상_제품이_표현된다() {
		RoutineCreateInput input = new RoutineCreateInput(List.of(
				new RoutineItemInput(RoutineItemUsage.DISCONTINUE, null, null, 999L),
				new RoutineItemInput(RoutineItemUsage.DISCONTINUE, null, null, 888L)
		));

		Routine routine = planner.plan(1L, 10L, startDate, input);

		assertThat(routine.discontinuedProductIds()).containsExactlyInAnyOrder(999L, 888L);
	}

	@Test
	void 입력에_없는_제품이_임의로_추가되지_않는다() {
		RoutineCreateInput input = new RoutineCreateInput(List.of(
				new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, 100L)
		));

		Routine routine = planner.plan(1L, 10L, startDate, input);

		assertThat(routine.continueItemsOf(1)).extracting(RoutineItem::getProductId).containsExactly(100L);
		assertThat(routine.discontinuedProductIds()).isEmpty();
	}

	@Test
	void 일부_day_slot이_비어도_예외_없이_생성된다_보유_제품_부족_케이스() {
		RoutineCreateInput input = new RoutineCreateInput(List.of(
				new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, 100L)
				// day1 evening, day2/3 전부 미채움
		));

		Routine routine = planner.plan(1L, 10L, startDate, input);

		assertThat(routine.continueItemsOf(1)).hasSize(1);
		assertThat(routine.continueItemsOf(2)).isEmpty();
		assertThat(routine.continueItemsOf(3)).isEmpty();
	}

	@Test
	void usage가_없으면_예외() {
		assertInvalid(new RoutineItemInput(null, 1, RoutineTimeSlot.MORNING, 100L));
	}

	@Test
	void productId가_없으면_예외() {
		assertInvalid(new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, null));
	}

	@Test
	void productId가_0이하면_예외() {
		assertInvalid(new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, 0L));
	}

	@Test
	void CONTINUE인데_dayNumber가_없으면_예외() {
		assertInvalid(new RoutineItemInput(RoutineItemUsage.CONTINUE, null, RoutineTimeSlot.MORNING, 100L));
	}

	@Test
	void CONTINUE인데_dayNumber가_범위_밖이면_예외() {
		assertInvalid(new RoutineItemInput(RoutineItemUsage.CONTINUE, 4, RoutineTimeSlot.MORNING, 100L));
	}

	@Test
	void CONTINUE인데_timeSlot이_없으면_예외() {
		assertInvalid(new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, null, 100L));
	}

	@Test
	void DISCONTINUE인데_dayNumber가_있으면_예외() {
		assertInvalid(new RoutineItemInput(RoutineItemUsage.DISCONTINUE, 1, null, 100L));
	}

	@Test
	void DISCONTINUE인데_timeSlot이_있으면_예외() {
		assertInvalid(new RoutineItemInput(RoutineItemUsage.DISCONTINUE, null, RoutineTimeSlot.MORNING, 100L));
	}

	@Test
	void 같은_day_timeSlot_제품_조합이_중복되면_예외() {
		RoutineCreateInput input = new RoutineCreateInput(List.of(
				new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, 100L),
				new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, 100L)
		));

		assertThatThrownBy(() -> planner.plan(1L, 10L, startDate, input))
				.isInstanceOf(AfterglowException.class)
				.extracting(e -> ((AfterglowException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	void 같은_제품이_discontinue에_중복되면_예외() {
		RoutineCreateInput input = new RoutineCreateInput(List.of(
				new RoutineItemInput(RoutineItemUsage.DISCONTINUE, null, null, 999L),
				new RoutineItemInput(RoutineItemUsage.DISCONTINUE, null, null, 999L)
		));

		assertThatThrownBy(() -> planner.plan(1L, 10L, startDate, input))
				.isInstanceOf(AfterglowException.class)
				.extracting(e -> ((AfterglowException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	void 같은_제품이_continue와_discontinue에_동시에_있으면_예외() {
		RoutineCreateInput input = new RoutineCreateInput(List.of(
				new RoutineItemInput(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, 100L),
				new RoutineItemInput(RoutineItemUsage.DISCONTINUE, null, null, 100L)
		));

		assertThatThrownBy(() -> planner.plan(1L, 10L, startDate, input))
				.isInstanceOf(AfterglowException.class)
				.extracting(e -> ((AfterglowException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	private void assertInvalid(RoutineItemInput itemInput) {
		RoutineCreateInput input = new RoutineCreateInput(List.of(itemInput));
		assertThatThrownBy(() -> planner.plan(1L, 10L, startDate, input))
				.isInstanceOf(AfterglowException.class)
				.extracting(e -> ((AfterglowException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_REQUEST);
	}
}
