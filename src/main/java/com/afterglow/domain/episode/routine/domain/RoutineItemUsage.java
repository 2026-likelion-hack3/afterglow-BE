package com.afterglow.domain.episode.routine.domain;

/**
 * Routine에 포함된 제품 하나가 이 3일간 어떻게 다뤄지는지 — 기능명세서 3.2(Manyfast F-VUQBTM).
 * 별도 Vanity Product 중단/재개 API 없이, "3일 루틴에서 사용중단으로 뜨면 사용중단된 것으로 간주"한다
 * (기획 확인, 2026-08-19) — 이 값 자체가 그 의사표시다.
 */
public enum RoutineItemUsage {
	/** 이 3일 동안 아침/저녁 특정 slot에서 계속 사용한다. */
	CONTINUE,
	/** 이 3일 동안 사용을 중단한다(특정 day/slot에 배치되지 않는다 — 기간 전체에 적용). */
	DISCONTINUE
}
