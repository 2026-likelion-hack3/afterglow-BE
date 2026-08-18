package com.afterglow.domain.episode.analysis.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 2026-08-13/2026-08-16 확정 규칙을 그대로 구현한 결정적(pure) 판정 엔진. Repository/Spring Bean/
 * 외부 호출/{@code LocalDateTime.now()}에 의존하지 않는다 — 같은 {@link AnalysisInput}이면 항상 같은
 * {@link AnalysisResult}를 반환한다.
 */
public final class RuleBasedCauseAnalysisEngine implements CauseAnalysisEngine {

	private static final long PRODUCT_TIMING_WINDOW_DAYS = 14;
	private static final double STRONG_MATCH_RATIO = 0.70;
	private static final double MEDIUM_MATCH_RATIO_MIN = 0.50;
	private static final double MEDIUM_MATCH_RATIO_MAX = 0.70;
	private static final int STRONG_MIN_OBSERVATIONS = 5;
	private static final int MEDIUM_MIN_OBSERVATIONS = 3;
	private static final int MEDIUM_MAX_OBSERVATIONS = 4;
	private static final double HOLD_GAP_THRESHOLD = 0.2;
	private static final double HIGH_CONFIDENCE_GAP_THRESHOLD = 0.4;

	private static final List<CandidateType> TYPE_PRIORITY =
			List.of(CandidateType.PRODUCT, CandidateType.COMBINATION, CandidateType.SLEEP, CandidateType.WEATHER);

	private static final Comparator<CandidateResult> RANKING_ORDER =
			Comparator.comparingInt(CandidateResult::score).reversed()
					.thenComparingInt(c -> TYPE_PRIORITY.indexOf(c.type()));

	@Override
	public AnalysisResult analyze(AnalysisInput input) {
		List<CandidateResult> candidates = new ArrayList<>();
		List<CandidateExclusion> exclusions = new ArrayList<>();

		collectProductCandidates(input, candidates, exclusions);
		collectCombinationCandidates(input, candidates, exclusions);
		collectObservationCandidate(CandidateType.SLEEP, input.sleep(), input.analysisDate(), candidates, exclusions);
		collectObservationCandidate(CandidateType.WEATHER, input.weather(), input.analysisDate(), candidates, exclusions);

		List<CandidateResult> ranked = candidates.stream().sorted(RANKING_ORDER).toList();
		return rank(ranked, exclusions);
	}

	// ------------------------------------------------------------------
	// candidate 수집 + 근거 강도
	//
	// "대상 없음 → NO_TARGET" 게이트는 4종 공통이다. 하지만 "7일 미만 → INSUFFICIENT_RECORDS" coverage
	// 게이트는 SLEEP/WEATHER 전용이다 — Manyfast 통합 분석(F-ZSPZHH, updateData) [후보 수집] 규칙 원문:
	// "일일 기록이 7일 미만이면 수면과 날씨를 제외하고 사유를 기록부족으로 남긴다"(2026-08-18 재확인).
	// PRODUCT/COMBINATION은 대상이 있으면 coverage 검사 없이 바로 근거 강도를 계산한다.
	// ------------------------------------------------------------------

	private void collectProductCandidates(
			AnalysisInput input, List<CandidateResult> candidates, List<CandidateExclusion> exclusions) {
		if (input.products().isEmpty()) {
			exclusions.add(new CandidateExclusion(CandidateType.PRODUCT, ExclusionReason.NO_TARGET));
			return;
		}
		for (ProductCandidateInput product : input.products()) {
			candidates.add(new CandidateResult(
					CandidateType.PRODUCT,
					productStrength(product),
					new TimingEvidence(product.productId(), product.usageStartDate(), product.symptomStartDate()),
					null));
		}
	}

	/**
	 * "product start가 symptom start보다 이전"이며 "14일 이내"를 [1, 14]일 전(양 끝 포함)으로 해석한다.
	 * 사용 시작일과 증상 시작일이 같은 날이면 "이전"이 아니므로 timing 불일치로 본다.
	 */
	private EvidenceStrength productStrength(ProductCandidateInput product) {
		long daysBeforeSymptom = ChronoUnit.DAYS.between(product.usageStartDate(), product.symptomStartDate());
		boolean timingMatches = daysBeforeSymptom > 0 && daysBeforeSymptom <= PRODUCT_TIMING_WINDOW_DAYS;
		if (!timingMatches) {
			return EvidenceStrength.WEAK;
		}
		return product.hasOtherProductChangesInWindow() ? EvidenceStrength.MEDIUM : EvidenceStrength.STRONG;
	}

	private void collectCombinationCandidates(
			AnalysisInput input, List<CandidateResult> candidates, List<CandidateExclusion> exclusions) {
		if (input.combinations().isEmpty()) {
			exclusions.add(new CandidateExclusion(CandidateType.COMBINATION, ExclusionReason.NO_TARGET));
			return;
		}
		for (CombinationCandidateInput combo : input.combinations()) {
			EvidenceStrength strength = switch (combo.conflictPlacement()) {
				case SAME_TIME_SLOT -> EvidenceStrength.STRONG;
				case SPLIT_AM_PM -> EvidenceStrength.MEDIUM;
				case NONE -> EvidenceStrength.WEAK;
			};
			candidates.add(new CandidateResult(
					CandidateType.COMBINATION,
					strength,
					new CombinationEvidence(combo.tagA(), combo.tagB(), combo.conflictPlacement()),
					null));
		}
	}

	private void collectObservationCandidate(
			CandidateType type, ObservationCandidateInput observation, LocalDate analysisDate,
			List<CandidateResult> candidates, List<CandidateExclusion> exclusions) {
		if (observation == null) {
			exclusions.add(new CandidateExclusion(type, ExclusionReason.NO_TARGET));
			return;
		}
		if (!observation.coverage().meetsSevenDayGate(analysisDate)) {
			exclusions.add(new CandidateExclusion(type, ExclusionReason.INSUFFICIENT_RECORDS));
			return;
		}
		candidates.add(new CandidateResult(
				type,
				observationStrength(observation),
				new FrequencyEvidence(observation.observationCount(), observation.matchedObservationCount()),
				observation.coverage().coverageDays(analysisDate)));
	}

	/**
	 * STRONG 조건(관측 5회 이상 + 일치 비율 70% 이상, 70% 포함)을 먼저 평가한 뒤 실패하면 MEDIUM 조건
	 * (관측 3~4회 또는 일치 비율 50~70%, 양 끝 포함)을 본다. 정확히 70%인데 관측이 5회 미만이면 STRONG
	 * 조건을 만족하지 못하므로 MEDIUM으로 fallback된다.
	 */
	private EvidenceStrength observationStrength(ObservationCandidateInput observation) {
		int observationCount = observation.observationCount();
		double matchRatio = observation.matchRatio();

		if (observationCount >= STRONG_MIN_OBSERVATIONS && matchRatio >= STRONG_MATCH_RATIO) {
			return EvidenceStrength.STRONG;
		}
		boolean observationCountInMediumRange =
				observationCount >= MEDIUM_MIN_OBSERVATIONS && observationCount <= MEDIUM_MAX_OBSERVATIONS;
		boolean matchRatioInMediumRange = matchRatio >= MEDIUM_MATCH_RATIO_MIN && matchRatio <= MEDIUM_MATCH_RATIO_MAX;
		if (observationCountInMediumRange || matchRatioInMediumRange) {
			return EvidenceStrength.MEDIUM;
		}
		return EvidenceStrength.WEAK;
	}

	// ------------------------------------------------------------------
	// ranking → gap → hold → confidence
	// ------------------------------------------------------------------

	private AnalysisResult rank(List<CandidateResult> ranked, List<CandidateExclusion> exclusions) {
		if (ranked.isEmpty()) {
			return AnalysisResult.hold(ranked, exclusions);
		}
		CandidateResult top1 = ranked.get(0);
		if (top1.strength() == EvidenceStrength.WEAK) {
			return AnalysisResult.hold(ranked, exclusions);
		}
		if (hasSameTypeTopTie(ranked, top1)) {
			return AnalysisResult.hold(ranked, exclusions);
		}

		double gap = ranked.size() == 1
				? 1.0
				: (double) (top1.score() - ranked.get(1).score()) / top1.score();
		if (gap < HOLD_GAP_THRESHOLD) {
			return AnalysisResult.hold(ranked, exclusions);
		}

		Confidence confidence = (top1.strength() == EvidenceStrength.STRONG && gap >= HIGH_CONFIDENCE_GAP_THRESHOLD)
				? Confidence.HIGH
				: Confidence.NORMAL;
		return AnalysisResult.determined(ranked, exclusions, top1, confidence);
	}

	/**
	 * top1과 같은 candidate type이면서 점수도 top1과 같은 후보가 2개 이상이면, 그 유형 안에서 우열을 가릴
	 * 수 없다는 뜻이다. 서로 다른 type 간의 동점은 이미 {@link #RANKING_ORDER}가 type 우선순위로 top1을
	 * 정했으므로 여기서는 same-type만 본다. 이 경우 top1은 정렬 편의를 위한 임시 값일 뿐 결과에는 반영되지
	 * 않는다 — HOLD 경로는 topCandidate를 항상 null로 반환한다.
	 */
	private boolean hasSameTypeTopTie(List<CandidateResult> ranked, CandidateResult top1) {
		long sameTypeTopCount = ranked.stream()
				.filter(c -> c.type() == top1.type() && c.score() == top1.score())
				.count();
		return sameTypeTopCount > 1;
	}
}
