package com.afterglow.domain.episode.routine.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

/**
 * {@link RoutineCreateInput}을 구조적으로 검증하고 {@link Routine}을 조립하는 순수 함수(Repository/Spring
 * 의존 없음, {@link com.afterglow.domain.episode.checkin.domain.Day3JudgmentEngine}과 동일한 스타일).
 *
 * <p>여기서 검증하는 건 "입력 자체가 일관적인가"뿐이다 — 실제로 그 productId가 호출자 계정의 보유
 * 제품인지, 중단 대상으로 표시된 제품이 Analysis가 지목한 원인 제품과 일치하는지는 Vanity/Analysis
 * 연동이 없어 이 계층에서 검증하지 못한다(README/보고서에 BLOCKED로 남긴다).
 */
public final class RoutinePlanner {

	public Routine plan(Long episodeId, Long accountId, LocalDate startDate, RoutineCreateInput input) {
		List<RoutineItemInput> itemInputs = input.items() == null ? List.of() : input.items();

		Set<String> seenContinueSlots = new HashSet<>();
		Set<Long> seenDiscontinueProducts = new HashSet<>();
		Set<Long> continueProductIds = new HashSet<>();
		Set<Long> discontinueProductIds = new HashSet<>();

		List<RoutineItem> items = itemInputs.stream()
				.map(itemInput -> toRoutineItem(itemInput, seenContinueSlots, seenDiscontinueProducts))
				.toList();

		for (RoutineItemInput itemInput : itemInputs) {
			if (itemInput.usage() == RoutineItemUsage.CONTINUE) {
				continueProductIds.add(itemInput.productId());
			} else if (itemInput.usage() == RoutineItemUsage.DISCONTINUE) {
				discontinueProductIds.add(itemInput.productId());
			}
		}
		continueProductIds.retainAll(discontinueProductIds);
		if (!continueProductIds.isEmpty()) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "같은 제품을 continue와 discontinue에 동시에 넣을 수 없습니다.");
		}

		return Routine.create(episodeId, accountId, startDate, items);
	}

	private RoutineItem toRoutineItem(RoutineItemInput itemInput, Set<String> seenContinueSlots, Set<Long> seenDiscontinueProducts) {
		if (itemInput.usage() == null) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "usage는 필수입니다.");
		}
		if (itemInput.productId() == null || itemInput.productId() <= 0) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "productId는 1 이상이어야 합니다.");
		}

		if (itemInput.usage() == RoutineItemUsage.CONTINUE) {
			return toContinueItem(itemInput, seenContinueSlots);
		}
		return toDiscontinueItem(itemInput, seenDiscontinueProducts);
	}

	private RoutineItem toContinueItem(RoutineItemInput itemInput, Set<String> seenContinueSlots) {
		if (itemInput.dayNumber() == null || itemInput.dayNumber() < 1 || itemInput.dayNumber() > Routine.DURATION_DAYS) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "dayNumber는 1~" + Routine.DURATION_DAYS + " 사이여야 합니다.");
		}
		if (itemInput.timeSlot() == null) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "CONTINUE 항목은 timeSlot이 필수입니다.");
		}

		String slotKey = itemInput.dayNumber() + ":" + itemInput.timeSlot() + ":" + itemInput.productId();
		if (!seenContinueSlots.add(slotKey)) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "같은 day/timeSlot/제품 조합이 중복됐습니다.");
		}

		return RoutineItem.continueItem(itemInput.dayNumber(), itemInput.timeSlot(), itemInput.productId());
	}

	private RoutineItem toDiscontinueItem(RoutineItemInput itemInput, Set<Long> seenDiscontinueProducts) {
		if (itemInput.dayNumber() != null || itemInput.timeSlot() != null) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "DISCONTINUE 항목은 dayNumber/timeSlot을 지정할 수 없습니다(기간 전체에 적용).");
		}
		if (!seenDiscontinueProducts.add(itemInput.productId())) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "같은 제품이 discontinue 목록에 중복됐습니다.");
		}

		return RoutineItem.discontinueItem(itemInput.productId());
	}
}
