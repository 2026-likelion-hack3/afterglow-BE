package com.afterglow.domain.episode.analysis.domain;

/** 공통 candidate gate에서 후보가 제외된 사유 — 기능명세서 2.4 후보 수집 조건. */
public enum ExclusionReason {
	/** 분석 대상 자체가 없음(예: 최근 새로 쓰는 제품 없음, 충돌 조합 없음). */
	NO_TARGET,
	/**
	 * 대상은 있지만 기록이 부족함. SLEEP/WEATHER는 최초 기록일부터 분석 기준일까지의 calendar-day
	 * coverage가 7일 미만인 경우(기존 규칙, 변경 없음). PRODUCT/COMBINATION은 관측 window 안에 실제
	 * CheckIn이 존재하는 날짜가 0일인 경우(2026-08-20 신설 — {@link ProductObservationWindow} 참고).
	 */
	INSUFFICIENT_RECORDS
}
