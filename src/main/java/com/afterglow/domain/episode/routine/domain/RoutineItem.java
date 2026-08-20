package com.afterglow.domain.episode.routine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Routine 안의 제품 배치 한 줄 — 기능명세서 3.2(Manyfast F-VUQBTM dataSpec: "일자, 시간대, 제품 순서").
 * Vanity {@code Product}를 compile-time으로 참조하지 않고 {@code productId}(값 참조)만 갖는다 — Vanity에
 * 아직 Repository가 없어 소유권/보유 여부를 서버가 검증할 수 없다(호출자가 보낸 값을 신뢰한다).
 *
 * <p>{@link RoutineItemUsage#CONTINUE}는 {@code dayNumber}/{@code timeSlot}이 필수(어느 날 아침/저녁에
 * 쓰는지), {@link RoutineItemUsage#DISCONTINUE}는 기간 전체에 적용되므로 둘 다 null이다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineItem {

	@Enumerated(EnumType.STRING)
	@Column(name = "usage_type", nullable = false)
	private RoutineItemUsage usage;

	@Column(name = "day_number")
	private Integer dayNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "time_slot")
	private RoutineTimeSlot timeSlot;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	private RoutineItem(RoutineItemUsage usage, Integer dayNumber, RoutineTimeSlot timeSlot, Long productId) {
		this.usage = usage;
		this.dayNumber = dayNumber;
		this.timeSlot = timeSlot;
		this.productId = productId;
	}

	public static RoutineItem continueItem(int dayNumber, RoutineTimeSlot timeSlot, Long productId) {
		return new RoutineItem(RoutineItemUsage.CONTINUE, dayNumber, timeSlot, productId);
	}

	public static RoutineItem discontinueItem(Long productId) {
		return new RoutineItem(RoutineItemUsage.DISCONTINUE, null, null, productId);
	}
}
