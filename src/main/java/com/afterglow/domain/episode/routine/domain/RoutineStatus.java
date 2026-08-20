package com.afterglow.domain.episode.routine.domain;

/**
 * Routine의 현재 상태 — 기능명세서 3.2(Manyfast F-VUQBTM).
 * Day3 판정(MAINTAIN/EXTEND/STOP) 이후 실제로 상태를 어떻게 전이시킬지(연장 일수, 중단 후 처리 등)는
 * 기획에 확정되지 않아 이번 범위에서는 다루지 않는다 — 지금은 ACTIVE만 존재한다.
 */
public enum RoutineStatus {
	ACTIVE
}
