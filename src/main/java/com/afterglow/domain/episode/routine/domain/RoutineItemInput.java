package com.afterglow.domain.episode.routine.domain;

/**
 * Routine 생성 입력의 한 줄. Vanity {@code Product}를 compile-time으로 참조하지 않기 위해 {@code productId}
 * (Long)만 받는다 — 실제로 그 제품이 호출자 계정의 보유 제품인지는 Vanity에 Repository가 아직 없어 이
 * 계층에서 검증하지 못한다(호출자가 이미 자신의 화장대 목록에서 고른 값이라고 신뢰한다).
 *
 * <p>{@link RoutineItemUsage#CONTINUE}는 {@code dayNumber}(1~3)/{@code timeSlot}이 필수이고,
 * {@link RoutineItemUsage#DISCONTINUE}는 기간 전체에 적용되므로 둘 다 비워야 한다 — {@link RoutinePlanner}가 이 조합을 검증한다.
 */
public record RoutineItemInput(
		RoutineItemUsage usage,
		Integer dayNumber,
		RoutineTimeSlot timeSlot,
		Long productId
) {
}
