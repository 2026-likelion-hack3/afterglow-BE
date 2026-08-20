package com.afterglow.domain.episode.intake.api;

import java.util.List;
import java.util.Set;

import com.afterglow.domain.episode.analysis.domain.CandidateType;
import com.afterglow.domain.vanity.InteractionTag;

/**
 * Figma E3("3회차를 모아봤어요") 최소 조회 응답 — 새 판정/추론 없이 기존 Episode/EpisodeAnalysisResult/
 * Routine/CheckIn/Vanity Product 데이터를 그대로 집계한다({@code EpisodeMultiSummaryService} 참고).
 *
 * <p><b>결제(2026-08-20)</b>: Figma는 이 데이터를 유료 CTA(4,900원) 뒤에 두지만, Manyfast는 결제/구독을
 * 여러 곳에서 명시적으로 범위 제외했다 — 이 API는 결제와 완전히 독립적으로 데이터만 반환한다. CTA 노출
 * 여부는 별도 기획 답변 대기다.
 *
 * @param sufficient                완료 Episode가 3건 이상인지(3건 미만이면 false, 나머지 필드는 빈 값)
 * @param completedEpisodeCount     실제 완료 Episode 개수(3 미만이어도 그대로 보고)
 * @param causeCounts                최근 완료 3건의 causeType별 등장 횟수 — "대표 원인" 하나로 줄이지 않고 전체를 그대로 반환한다
 * @param averageImprovementDay     Day1~3 중 첫 IMPROVED dayNumber(1.0~3.0)의 평균 — 존재하는 값만 평균하며, 하나도 없으면 null. Figma 예시("평균 4일")는 3일 고정 스키마로는 재현 불가능해 실제 계산값만 반환한다
 * @param repeatedProductPatterns   causeType=PRODUCT인 episode마다의 제품 정보(그룹/집계 없이 원본 그대로) — "몇 번 반복됐다"는 판단은 새 정책이 필요해 만들지 않았다
 * @param combinationPatterns       causeType=COMBINATION인 episode마다의 태그 쌍(evidenceTagA/evidenceTagB 그대로)
 */
public record EpisodeMultiSummaryResponse(
		boolean sufficient,
		int completedEpisodeCount,
		List<CauseCount> causeCounts,
		Double averageImprovementDay,
		List<ProductCausePattern> repeatedProductPatterns,
		List<CombinationCausePattern> combinationPatterns
) {

	public record CauseCount(CandidateType causeType, int count) {
	}

	/** product는 제품이 삭제됐거나 조회에 실패하면 null이다 — productId는 항상 저장된 원본 값을 그대로 보여준다. */
	public record ProductCausePattern(Long episodeId, Long productId, ProductDetail product) {
	}

	/** keyIngredients를 파싱하지 않는다 — 이미 저장된 type/interactionTags만 그대로 노출한다(새 추론 없음). */
	public record ProductDetail(String type, Set<InteractionTag> interactionTags) {
	}

	public record CombinationCausePattern(Long episodeId, String tagA, String tagB) {
	}
}
