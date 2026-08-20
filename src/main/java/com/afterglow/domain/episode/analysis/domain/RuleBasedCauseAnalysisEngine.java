package com.afterglow.domain.episode.analysis.domain;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 2026-08-13/2026-08-16/2026-08-19 확정 규칙을 그대로 구현한 결정적(pure) 판정 엔진. Repository/Spring
 * Bean/외부 호출/{@code LocalDateTime.now()}에 의존하지 않는다 — 같은 {@link AnalysisInput}이면 항상 같은
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

	private static final List<CandidateType> TYPE_PRIORITY =
			List.of(CandidateType.PRODUCT, CandidateType.COMBINATION, CandidateType.SLEEP, CandidateType.WEATHER);

	/**
	 * 순위는 점수 격차 계산이 아니라 정렬 규칙이다(Manyfast 통합 분석 F-ZSPZHH 최신 확정, 2026-08-19):
	 * 1차 근거 강도(강&gt;중&gt;약) → 2차 강도가 같으면 type priority(되돌리기 쉬운 순서) → 3차 type과
	 * 강도까지 같으면 같은 종류 안의 tie-break(제품=기준일 최근순, 조합=충돌 강도순, 수면/날씨는 후보가
	 * 타입당 최대 하나뿐이라 같은 타입끼리 동점 자체가 발생하지 않는다). 이 셋을 모두 적용해도 1·2위가
	 * 구분되지 않으면(comparator가 0을 반환하면) 그건 "정렬 규칙을 끝까지 적용해도 1순위를 가릴 수 없는"
	 * 상태이며 {@link #rank}에서 HOLD로 처리한다.
	 */
	private static final Comparator<CandidateResult> RANKING_ORDER =
			Comparator.comparing(CandidateResult::strength)
					.thenComparingInt(c -> TYPE_PRIORITY.indexOf(c.type()))
					.thenComparing(RuleBasedCauseAnalysisEngine::compareSameTypeTieBreak);

	@Override
	public AnalysisResult analyze(AnalysisInput input) {
		List<CandidateResult> candidates = new ArrayList<>();
		List<CandidateExclusion> exclusions = new ArrayList<>();

		collectProductCandidates(input, candidates, exclusions);
		collectCombinationCandidates(input, candidates, exclusions);
		collectObservationCandidate(CandidateType.SLEEP, input.sleep(), candidates, exclusions);
		collectObservationCandidate(CandidateType.WEATHER, input.weather(), candidates, exclusions);

		List<CandidateResult> ranked = candidates.stream().sorted(RANKING_ORDER).toList();
		return rank(ranked, exclusions);
	}

	// ------------------------------------------------------------------
	// candidate 수집 + 근거 강도
	//
	// "대상 없음 → NO_TARGET" 게이트는 4종 공통이다. "기록 부족 → INSUFFICIENT_RECORDS" coverage 게이트도
	// 4종 공통으로 "실제 분석 가능한 record/observation이 존재하는 날짜 수" 기준이다(2026-08-20 정정 —
	// SLEEP/WEATHER도 원래는 calendar-day span이었으나 record 건수 기준으로 바뀌었다. 임계값 7 자체는
	// 그대로다). PRODUCT/COMBINATION은 0이면 후보별로 개별 제외, SLEEP/WEATHER는 7 미만이면 제외.
	// ------------------------------------------------------------------

	private void collectProductCandidates(
			AnalysisInput input, List<CandidateResult> candidates, List<CandidateExclusion> exclusions) {
		if (input.products().isEmpty()) {
			exclusions.add(new CandidateExclusion(CandidateType.PRODUCT, ExclusionReason.NO_TARGET));
			return;
		}
		for (ProductCandidateInput product : input.products()) {
			if (product.coverageDays() == 0) {
				exclusions.add(new CandidateExclusion(
						CandidateType.PRODUCT, ExclusionReason.INSUFFICIENT_RECORDS, String.valueOf(product.productId())));
				continue;
			}
			candidates.add(new CandidateResult(
					CandidateType.PRODUCT,
					productStrength(product),
					new TimingEvidence(product.productId(), product.usageStartDate(), product.symptomStartDate()),
					product.coverageDays()));
		}
	}

	/**
	 * {@link ReferenceCertainty#FALLBACK}(개봉 시기 자체가 없어 등록일로 대체)이면 timing 계산 없이 WEAK로
	 * 고정한다. 그 외(현재는 {@link ReferenceCertainty#ESTIMATED} 하나뿐 — Vanity가 정확한 사용 시작일을
	 * 아직 주지 않는다)는 기존 timing 규칙으로 강도를 계산한 뒤 한 단계 downgrade한다(2026-08-20 확정).
	 * symptomStartDate는 onsetPeriod → 날짜 환산 규칙이 확정되며(2026-08-20) 모든 onsetPeriod 값에서 실제
	 * 날짜로 계산되므로, 여기서는 더 이상 별도로 신뢰도를 검사하지 않는다.
	 */
	private EvidenceStrength productStrength(ProductCandidateInput product) {
		if (product.referenceCertainty() == ReferenceCertainty.FALLBACK) {
			return EvidenceStrength.WEAK;
		}
		EvidenceStrength timingStrength = productTimingStrength(product);
		return product.referenceCertainty() == ReferenceCertainty.ESTIMATED ? timingStrength.downgrade() : timingStrength;
	}

	/**
	 * "product start가 symptom start보다 이전"이며 "14일 이내"를 [1, 14]일 전(양 끝 포함)으로 해석한다.
	 * 사용 시작일과 증상 시작일이 같은 날이면 "이전"이 아니므로 timing 불일치로 본다.
	 */
	private EvidenceStrength productTimingStrength(ProductCandidateInput product) {
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
			if (combo.coverageDays() == 0) {
				exclusions.add(new CandidateExclusion(
						CandidateType.COMBINATION, ExclusionReason.INSUFFICIENT_RECORDS, combo.tagA() + "+" + combo.tagB()));
				continue;
			}
			EvidenceStrength strength = switch (combo.conflictPlacement()) {
				case SAME_TIME_SLOT -> EvidenceStrength.STRONG;
				case SPLIT_AM_PM -> EvidenceStrength.MEDIUM;
				case NONE -> EvidenceStrength.WEAK;
			};
			candidates.add(new CandidateResult(
					CandidateType.COMBINATION,
					strength,
					new CombinationEvidence(combo.tagA(), combo.tagB(), combo.conflictPlacement()),
					combo.coverageDays()));
		}
	}

	private void collectObservationCandidate(
			CandidateType type, ObservationCandidateInput observation,
			List<CandidateResult> candidates, List<CandidateExclusion> exclusions) {
		if (observation == null) {
			exclusions.add(new CandidateExclusion(type, ExclusionReason.NO_TARGET));
			return;
		}
		if (!observation.coverage().meetsSevenDayGate()) {
			exclusions.add(new CandidateExclusion(type, ExclusionReason.INSUFFICIENT_RECORDS));
			return;
		}
		candidates.add(new CandidateResult(
				type,
				observationStrength(observation),
				new FrequencyEvidence(observation.observationCount(), observation.matchedObservationCount()),
				observation.coverage().coverageDays()));
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
	// ranking → hold → confidence
	// ------------------------------------------------------------------

	/**
	 * "정렬 규칙을 끝까지 적용해도 1순위를 가릴 수 없으면 보류한다"(F-ZSPZHH 최신 확정) — {@code ranked}는
	 * 이미 {@link #RANKING_ORDER}로 정렬돼 있으므로, 1위와 2위를 그 순서 그대로 다시 비교해서 0(구분 불가)이
	 * 나오면 그게 곧 "끝까지 적용해도 못 가른" 상태다. 서로 다른 type끼리는 이미 type priority가 갈라놓으므로
	 * 여기서 0이 나오는 경우는 실질적으로 같은 type 안에서 강도·tie-break까지 전부 같을 때뿐이다.
	 */
	private AnalysisResult rank(List<CandidateResult> ranked, List<CandidateExclusion> exclusions) {
		if (ranked.isEmpty()) {
			return AnalysisResult.hold(ranked, exclusions);
		}
		CandidateResult top1 = ranked.get(0);
		if (top1.strength() == EvidenceStrength.WEAK) {
			return AnalysisResult.hold(ranked, exclusions);
		}
		CandidateResult second = ranked.size() > 1 ? ranked.get(1) : null;
		if (second != null && RANKING_ORDER.compare(top1, second) == 0) {
			return AnalysisResult.hold(ranked, exclusions);
		}

		Confidence confidence = (top1.strength() == EvidenceStrength.STRONG && (second == null || second.strength() == EvidenceStrength.WEAK))
				? Confidence.HIGH
				: Confidence.NORMAL;
		return AnalysisResult.determined(ranked, exclusions, top1, confidence);
	}

	/**
	 * 같은 type·같은 강도인 두 후보의 3차 tie-break(F-ZSPZHH 최신 확정) — 제품은 기준일(현재는
	 * {@link ProductCandidateInput#usageStartDate()} 그대로, 추정치 환산은 이번 범위 밖)이 더 최근인 쪽을
	 * 우선한다. 조합은 충돌 강도({@link ConflictPlacement} 선언 순서: SAME_TIME_SLOT &gt; SPLIT_AM_PM &gt;
	 * NONE)가 더 센 쪽을 우선한다. 수면/날씨는 {@link AnalysisInput}에 타입당 후보가 최대 하나뿐이라 같은
	 * type끼리 동점 자체가 나올 수 없으므로 별도 tie-break가 없다 — 명세에도 정의돼 있지 않다.
	 */
	private static int compareSameTypeTieBreak(CandidateResult a, CandidateResult b) {
		return switch (a.type()) {
			case PRODUCT -> ((TimingEvidence) b.evidence()).usageStartDate()
					.compareTo(((TimingEvidence) a.evidence()).usageStartDate());
			case COMBINATION -> ((CombinationEvidence) a.evidence()).conflictPlacement()
					.compareTo(((CombinationEvidence) b.evidence()).conflictPlacement());
			case SLEEP, WEATHER -> 0;
		};
	}
}
