package com.afterglow.domain.episode.routine.api;

import com.afterglow.domain.episode.routine.domain.RoutineItemUsage;
import com.afterglow.domain.episode.routine.domain.RoutineTimeSlot;

import jakarta.validation.constraints.NotNull;

/**
 * CONTINUE는 dayNumber(1~3)/timeSlot이 필수, DISCONTINUE는 기간 전체에 적용되므로 둘 다 비운다 —
 * 세부 조합 검증은 {@link com.afterglow.domain.episode.routine.domain.RoutinePlanner}에서 한다.
 */
public record RoutineItemRequest(
		@NotNull RoutineItemUsage usage,
		Integer dayNumber,
		RoutineTimeSlot timeSlot,
		@NotNull Long productId
) {
}
