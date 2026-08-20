package com.afterglow.domain.episode.routine.domain;

import java.time.LocalDate;

/**
 * 계정의 Routine 이력에서 특정 제품에 벌어진 사건 하나 — 기획 확정(2026-08-19): "3일 루틴에서 사용중단
 * 대상으로 지정하면 중단된 것", "중단됐던 제품이 이후 Routine에 다시 포함되면 재개된 것, 재개일은 그
 * Routine의 시작일".
 *
 * @param usage    {@link RoutineItemUsage#DISCONTINUE}(이 시점에 중단) 또는 {@link RoutineItemUsage#CONTINUE}(이 시점에 사용)
 * @param date     이 사건이 벌어진 Routine의 시작일
 * @param resumed  직전 사건이 DISCONTINUE였고 이번이 CONTINUE인 경우에만 true — "재개" 사건
 */
public record ProductUsageEvent(RoutineItemUsage usage, LocalDate date, boolean resumed) {
}
