package com.afterglow.domain.episode.analysis.domain;

/**
 * 후보별 근거 강도 — 강 &gt; 중 &gt; 약 순으로 정렬된다(선언 순서가 곧 우선순위, enum 자연 순서로 비교
 * 가능). 점수(3/2/1) 환산 후 격차를 계산하는 방식은 순위/확신 단계 산정에 더 이상 쓰이지 않는다 —
 * Manyfast 통합 분석(F-ZSPZHH) 최신 확정: "강·중·약을 점수로 바꿔 나누는 격차 계산은 쓰지 않는다. 값이
 * 세 가지뿐이라 임계값이 의미를 잃고, 같은 종류 안의 동점을 가를 수 없기 때문이다"(2026-08-19).
 */
public enum EvidenceStrength {
	STRONG,
	MEDIUM,
	WEAK
}
