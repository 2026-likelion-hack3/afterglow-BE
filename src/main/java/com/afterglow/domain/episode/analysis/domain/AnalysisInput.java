package com.afterglow.domain.episode.analysis.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * {@link CauseAnalysisEngine}의 유일한 입력. 엔진은 이 값만으로 결정적으로 결과를 산출하며, 어떤
 * Repository/외부 시스템도 조회하지 않는다. 대상 자체가 없는 후보 타입은 빈 리스트(product/combination)
 * 또는 null(sleep/weather)로 표현한다 — 그 자체가 공통 gate의 "대상 없음" 조건이다.
 *
 * @param analysisDate    분석 기준일(보통 episode.createdAt의 날짜)
 * @param products        특정 제품 후보 목록
 * @param combinations    제품 조합 후보 목록
 * @param sleep           수면 후보(대상 없으면 null)
 * @param weather         날씨 후보(대상 없으면 null)
 */
public record AnalysisInput(
		LocalDate analysisDate,
		List<ProductCandidateInput> products,
		List<CombinationCandidateInput> combinations,
		ObservationCandidateInput sleep,
		ObservationCandidateInput weather
) {
	public AnalysisInput {
		products = products == null ? List.of() : List.copyOf(products);
		combinations = combinations == null ? List.of() : List.copyOf(combinations);
	}
}
