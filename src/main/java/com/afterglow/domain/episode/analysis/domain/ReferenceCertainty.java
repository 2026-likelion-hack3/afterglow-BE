package com.afterglow.domain.episode.analysis.domain;

/**
 * 제품 후보의 {@code usageStartDate}가 실측값인지 추정값인지 — Vanity는 정확한 사용 시작일을 주지 않고
 * "개봉 시기" 범주(OpeningPeriod)만 주므로, 이 범주를 날짜로 환산한 값은 항상 추정값이다(2026-08-20 확정).
 */
public enum ReferenceCertainty {
	/**
	 * 실측 기준일 — downgrade 없음. 현재 실제 로더(Vanity)는 이 값을 만들지 않는다(정확한 사용 시작일
	 * 데이터가 없음). timing 규칙 자체(강/중/약 판정)를 downgrade와 독립적으로 검증하는 테스트 기준값으로 남긴다.
	 */
	EXACT,
	/** OpeningPeriod 범주를 날짜로 환산한 추정 기준일 — 근거 강도를 한 단계 downgrade한다(강→중, 중→약, 약→약). */
	ESTIMATED,
	/** OpeningPeriod 자체가 없어 제품 등록일로 대체한 기준일 — 강도를 WEAK로 고정한다. */
	FALLBACK
}
