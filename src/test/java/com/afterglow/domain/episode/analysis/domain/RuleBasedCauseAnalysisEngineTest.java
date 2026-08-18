package com.afterglow.domain.episode.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class RuleBasedCauseAnalysisEngineTest {

	private static final LocalDate ANALYSIS_DATE = LocalDate.of(2026, 8, 16);
	private static final LocalDate SYMPTOM_START = LocalDate.of(2026, 8, 16);

	/** analysisDate 기준 정확히 7 calendar days 커버리지(SLEEP/WEATHER 게이트를 항상 통과) — 규칙 자체를 검증하는 테스트에서 게이트는 관심사가 아니므로 고정값을 쓴다. */
	private static final RecordCoverage SUFFICIENT_COVERAGE = new RecordCoverage(ANALYSIS_DATE.minusDays(6));
	private static final RecordCoverage INSUFFICIENT_COVERAGE = new RecordCoverage(ANALYSIS_DATE.minusDays(5));

	private final RuleBasedCauseAnalysisEngine engine = new RuleBasedCauseAnalysisEngine();

	// ------------------------------------------------------------------
	// 공통 NO_TARGET 게이트 (4종 공통)
	// ------------------------------------------------------------------

	@Test
	void 대상이_없으면_해당_타입_후보가_생성되지_않는다() {
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.candidates()).isEmpty();
		assertThat(result.hold()).isTrue();
	}

	// ------------------------------------------------------------------
	// PRODUCT/COMBINATION은 coverage 게이트가 없다(2026-08-18 확정, Manyfast 통합 분석 F-ZSPZHH
	// updateData [후보 수집] 규칙: 7일 게이트는 수면/날씨에만 명시돼 있음).
	// ------------------------------------------------------------------

	@Test
	void product_대상이_있으면_coverage_개념_없이_바로_candidate로_채택된다() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.minusDays(10), 1);

		assertThat(result.candidates()).hasSize(1);
		assertThat(result.candidates().get(0).type()).isEqualTo(CandidateType.PRODUCT);
		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.STRONG);
		assertThat(result.exclusions()).noneMatch(e -> e.type() == CandidateType.PRODUCT);
	}

	@Test
	void combination_대상이_있으면_coverage_개념_없이_바로_candidate로_채택된다() {
		AnalysisResult result = analyzeSingleCombination(ConflictPlacement.SAME_TIME_SLOT);

		assertThat(result.candidates()).hasSize(1);
		assertThat(result.candidates().get(0).type()).isEqualTo(CandidateType.COMBINATION);
		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.STRONG);
		assertThat(result.exclusions()).noneMatch(e -> e.type() == CandidateType.COMBINATION);
	}

	// ------------------------------------------------------------------
	// 특정 제품
	// ------------------------------------------------------------------

	@Test
	void 제품_사용_시작이_증상보다_이전_14일_이내이고_다른_변경이_없으면_STRONG() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.minusDays(10), 1);

		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.STRONG);
	}

	@Test
	void timing은_맞지만_같은_기간_2개_이상_제품이_변경되면_MEDIUM() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.minusDays(10), 2);

		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.MEDIUM);
	}

	@Test
	void timing이_어긋나면_WEAK() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.plusDays(1), 1);

		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.WEAK);
	}

	@Test
	void 정확히_14일_전_시작은_STRONG_경계를_통과한다() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.minusDays(14), 1);

		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.STRONG);
	}

	@Test
	void 사용_시작_15일_전은_14일_윈도우를_벗어나_WEAK() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.minusDays(15), 1);

		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.WEAK);
	}

	private AnalysisResult analyzeSingleProduct(LocalDate usageStartDate, int changedProductCountInWindow) {
		ProductCandidateInput product = new ProductCandidateInput(
				1L, usageStartDate, SYMPTOM_START, changedProductCountInWindow);
		return engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(product), List.of(), null, null));
	}

	// ------------------------------------------------------------------
	// 제품 조합
	// ------------------------------------------------------------------

	@Test
	void 같은_time_slot에_충돌_태그가_배치되면_STRONG() {
		AnalysisResult result = analyzeSingleCombination(ConflictPlacement.SAME_TIME_SLOT);

		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.STRONG);
	}

	@Test
	void 충돌이_아침_저녁으로_분리되면_MEDIUM() {
		AnalysisResult result = analyzeSingleCombination(ConflictPlacement.SPLIT_AM_PM);

		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.MEDIUM);
	}

	@Test
	void 충돌이_없으면_WEAK() {
		AnalysisResult result = analyzeSingleCombination(ConflictPlacement.NONE);

		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.WEAK);
	}

	private AnalysisResult analyzeSingleCombination(ConflictPlacement placement) {
		CombinationCandidateInput combination = new CombinationCandidateInput("RETINOL", "ACID", placement);
		return engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(combination), null, null));
	}

	// ------------------------------------------------------------------
	// 수면 / 날씨
	// ------------------------------------------------------------------

	@Test
	void sleep_coverage가_6일이면_INSUFFICIENT_RECORDS로_제외된다() {
		ObservationCandidateInput sleep = new ObservationCandidateInput(10, 7, INSUFFICIENT_COVERAGE);
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), sleep, null));

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.SLEEP, ExclusionReason.INSUFFICIENT_RECORDS));
		assertThat(result.candidates()).noneMatch(c -> c.type() == CandidateType.SLEEP);
	}

	@Test
	void sleep_coverage가_정확히_7일이면_candidate로_채택되어_strength가_계산된다() {
		ObservationCandidateInput sleep = new ObservationCandidateInput(10, 7, SUFFICIENT_COVERAGE);
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), sleep, null));

		assertThat(result.candidates()).hasSize(1);
		assertThat(result.candidates().get(0).type()).isEqualTo(CandidateType.SLEEP);
		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.STRONG);
	}

	@Test
	void weather_coverage가_6일이면_INSUFFICIENT_RECORDS로_제외된다() {
		ObservationCandidateInput weather = new ObservationCandidateInput(10, 7, INSUFFICIENT_COVERAGE);
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, weather));

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.WEATHER, ExclusionReason.INSUFFICIENT_RECORDS));
		assertThat(result.candidates()).noneMatch(c -> c.type() == CandidateType.WEATHER);
	}

	@Test
	void weather_coverage가_정확히_7일이면_candidate로_채택되어_strength가_계산된다() {
		ObservationCandidateInput weather = new ObservationCandidateInput(10, 7, SUFFICIENT_COVERAGE);
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, weather));

		assertThat(result.candidates()).hasSize(1);
		assertThat(result.candidates().get(0).type()).isEqualTo(CandidateType.WEATHER);
		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.STRONG);
	}

	@Test
	void 관측_5회_이상이고_일치율이_정확히_70퍼센트면_STRONG() {
		assertThat(sleepStrength(10, 7)).isEqualTo(EvidenceStrength.STRONG);
	}

	@Test
	void 관측_5회_이상이고_일치율이_70퍼센트_초과면_STRONG() {
		assertThat(sleepStrength(6, 6)).isEqualTo(EvidenceStrength.STRONG);
	}

	@Test
	void 관측이_3회면_일치율과_무관하게_MEDIUM() {
		assertThat(sleepStrength(3, 1)).isEqualTo(EvidenceStrength.MEDIUM);
	}

	@Test
	void 관측이_4회면_일치율과_무관하게_MEDIUM() {
		assertThat(sleepStrength(4, 1)).isEqualTo(EvidenceStrength.MEDIUM);
	}

	@Test
	void 일치율이_정확히_50퍼센트면_MEDIUM() {
		assertThat(sleepStrength(10, 5)).isEqualTo(EvidenceStrength.MEDIUM);
	}

	/**
	 * 정수 관측 횟수에서는 observations&lt;5이면서 matchRatio가 정확히 0.70인 조합을 만들 수 없다
	 * (0.70을 정수 분수로 나타내려면 분모가 최소 10 이상이어야 한다). 대신 STRONG 문턱(70%)을 만족하고도
	 * 남는 높은 일치율(75%)이 관측 부족(4회) 때문에 STRONG이 아니라 MEDIUM으로 fallback되는지를 검증한다.
	 */
	@Test
	void 일치율이_70퍼센트_이상이어도_관측이_5회_미만이면_MEDIUM() {
		assertThat(sleepStrength(4, 3)).isEqualTo(EvidenceStrength.MEDIUM);
	}

	@Test
	void 관측도_적고_일치율도_50퍼센트_미만이면_WEAK() {
		assertThat(sleepStrength(10, 4)).isEqualTo(EvidenceStrength.WEAK);
	}

	@Test
	void 날씨_후보도_동일한_규칙이_적용된다() {
		ObservationCandidateInput weather = new ObservationCandidateInput(10, 7, SUFFICIENT_COVERAGE);
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, weather));

		assertThat(result.candidates().get(0).type()).isEqualTo(CandidateType.WEATHER);
		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.STRONG);
	}

	private EvidenceStrength sleepStrength(int observationCount, int matchedObservationCount) {
		ObservationCandidateInput sleep = new ObservationCandidateInput(observationCount, matchedObservationCount, SUFFICIENT_COVERAGE);
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), sleep, null));
		return result.candidates().get(0).strength();
	}

	// ------------------------------------------------------------------
	// ranking — strength → type priority → same-type tie-break
	// (2026-08-19, Manyfast F-ZSPZHH 최신 확정: 점수 격차 계산 폐기, 정렬 규칙으로 대체)
	// ------------------------------------------------------------------

	@Test
	void PRODUCT와_COMBINATION이_같은_강도면_type_priority로_PRODUCT가_선택되고_confidence는_NORMAL이다() {
		AnalysisResult result = analyzeProductAndCombination(EvidenceStrength.STRONG, EvidenceStrength.STRONG);

		assertThat(result.hold()).isFalse();
		assertThat(result.topCandidate().type()).isEqualTo(CandidateType.PRODUCT);
		assertThat(result.confidence()).isEqualTo(Confidence.NORMAL);
	}

	@Test
	void COMBINATION과_SLEEP이_같은_강도면_type_priority로_COMBINATION이_선택된다() {
		CombinationCandidateInput combination = new CombinationCandidateInput("RETINOL", "ACID", ConflictPlacement.SAME_TIME_SLOT);
		ObservationCandidateInput sleep = new ObservationCandidateInput(10, 7, SUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(combination), sleep, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.hold()).isFalse();
		assertThat(result.topCandidate().type()).isEqualTo(CandidateType.COMBINATION);
	}

	@Test
	void SLEEP과_WEATHER가_같은_강도면_type_priority로_SLEEP이_선택된다() {
		ObservationCandidateInput sleep = new ObservationCandidateInput(10, 7, SUFFICIENT_COVERAGE);
		ObservationCandidateInput weather = new ObservationCandidateInput(10, 7, SUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), sleep, weather);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.hold()).isFalse();
		assertThat(result.topCandidate().type()).isEqualTo(CandidateType.SLEEP);
	}

	@Test
	void PRODUCT가_같은_강도로_동점이면_usageStartDate가_최근인_후보가_선택된다() {
		ProductCandidateInput older = new ProductCandidateInput(1L, SYMPTOM_START.minusDays(10), SYMPTOM_START, 1);
		ProductCandidateInput moreRecent = new ProductCandidateInput(2L, SYMPTOM_START.minusDays(5), SYMPTOM_START, 1);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(older, moreRecent), List.of(), null, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.hold()).isFalse();
		assertThat(((TimingEvidence) result.topCandidate().evidence()).productId()).isEqualTo(2L);
	}

	@Test
	void PRODUCT가_강도와_usageStartDate까지_같으면_다른_tie_break가_없어_HOLD이다() {
		ProductCandidateInput productA = new ProductCandidateInput(1L, SYMPTOM_START.minusDays(10), SYMPTOM_START, 1);
		ProductCandidateInput productB = new ProductCandidateInput(2L, SYMPTOM_START.minusDays(10), SYMPTOM_START, 1);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(productA, productB), List.of(), null, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.hold()).isTrue();
		assertThat(result.topCandidate()).isNull();
		assertThat(result.confidence()).isNull();
	}

	/**
	 * conflictPlacement tie-break(SAME_TIME_SLOT &gt; SPLIT_AM_PM &gt; NONE) 자체가 명시적으로 다른 값을
	 * 골라내는 케이스는 만들 수 없다 — {@code conflictPlacement}가 strength를 그대로 결정하므로(SAME_TIME_SLOT
	 * →강, SPLIT_AM_PM→중, NONE→약) "같은 강도인데 conflictPlacement가 다른" 조합 후보는 지금의 강도 계산
	 * 규칙상 존재할 수 없다. 그래서 tie-break가 "적용됐을 때 다른 값을 고른다"는 것 자체는 이번 테스트로
	 * 검증하지 않고, 강도와 conflictPlacement가 완전히 같은 두 후보가 tie-break까지 적용해도 못 갈려 HOLD가
	 * 되는지만 검증한다.
	 */
	@Test
	void COMBINATION이_강도와_conflictPlacement까지_같으면_HOLD이다() {
		CombinationCandidateInput comboA = new CombinationCandidateInput("RETINOL", "ACID", ConflictPlacement.SAME_TIME_SLOT);
		CombinationCandidateInput comboB = new CombinationCandidateInput("VITAMIN_C", "HIGH_CONCENTRATION", ConflictPlacement.SAME_TIME_SLOT);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(comboA, comboB), null, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.hold()).isTrue();
		assertThat(result.topCandidate()).isNull();
		assertThat(result.confidence()).isNull();
	}

	@Test
	void 다른_타입이_MEDIUM으로_동점이어도_HOLD가_아니라_type_priority로_결정된다() {
		CombinationCandidateInput combination = new CombinationCandidateInput("RETINOL", "ACID", ConflictPlacement.SPLIT_AM_PM);
		ObservationCandidateInput sleep = new ObservationCandidateInput(3, 1, SUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(combination), sleep, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.hold()).isFalse();
		assertThat(result.topCandidate().type()).isEqualTo(CandidateType.COMBINATION);
		assertThat(result.confidence()).isEqualTo(Confidence.NORMAL);
	}

	/** WEAK를 만들기 위해 timing이 어긋난 제품(WEAK) / 충돌 없는 조합(WEAK)을 각각 원하는 강도로 만들어 조합한다. */
	private AnalysisResult analyzeProductAndCombination(EvidenceStrength productStrength, EvidenceStrength combinationStrength) {
		ProductCandidateInput product = toProductWithStrength(productStrength);
		CombinationCandidateInput combination = toCombinationWithStrength(combinationStrength);
		return engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(product), List.of(combination), null, null));
	}

	private ProductCandidateInput toProductWithStrength(EvidenceStrength strength) {
		return switch (strength) {
			case STRONG -> new ProductCandidateInput(1L, SYMPTOM_START.minusDays(10), SYMPTOM_START, 1);
			case MEDIUM -> new ProductCandidateInput(1L, SYMPTOM_START.minusDays(10), SYMPTOM_START, 2);
			case WEAK -> new ProductCandidateInput(1L, SYMPTOM_START.plusDays(1), SYMPTOM_START, 1);
		};
	}

	private CombinationCandidateInput toCombinationWithStrength(EvidenceStrength strength) {
		ConflictPlacement placement = switch (strength) {
			case STRONG -> ConflictPlacement.SAME_TIME_SLOT;
			case MEDIUM -> ConflictPlacement.SPLIT_AM_PM;
			case WEAK -> ConflictPlacement.NONE;
		};
		return new CombinationCandidateInput("RETINOL", "ACID", placement);
	}

	// ------------------------------------------------------------------
	// HOLD
	// ------------------------------------------------------------------

	@Test
	void 후보_목록이_비어있으면_HOLD이다() {
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null));

		assertThat(result.hold()).isTrue();
		assertThat(result.topCandidate()).isNull();
		assertThat(result.confidence()).isNull();
	}

	@Test
	void top_후보가_WEAK이면_HOLD이고_confidence가_없다() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.plusDays(1), 1);

		assertThat(result.hold()).isTrue();
		assertThat(result.topCandidate()).isNull();
		assertThat(result.confidence()).isNull();
	}

	// ------------------------------------------------------------------
	// confidence (2026-08-19, Manyfast F-ZSPZHH 최신 확정: "1순위가 강이고 2순위가 약이거나 없으면
	// 높음이다. 그 외는 보통이다." — top1이 WEAK인 경우는 위에서 이미 HOLD로 걸러진다)
	// ------------------------------------------------------------------

	@Test
	void top이_STRONG이고_second가_없으면_HIGH이다() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.minusDays(10), 1);

		assertThat(result.candidates()).hasSize(1);
		assertThat(result.hold()).isFalse();
		assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
	}

	@Test
	void top이_STRONG이고_second가_WEAK이면_HIGH이다() {
		AnalysisResult result = analyzeProductAndCombination(EvidenceStrength.STRONG, EvidenceStrength.WEAK);

		assertThat(result.hold()).isFalse();
		assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
	}

	@Test
	void top이_STRONG이고_second가_MEDIUM이면_NORMAL이다() {
		AnalysisResult result = analyzeProductAndCombination(EvidenceStrength.STRONG, EvidenceStrength.MEDIUM);

		assertThat(result.hold()).isFalse();
		assertThat(result.confidence()).isEqualTo(Confidence.NORMAL);
	}

	@Test
	void top이_MEDIUM이면_second와_무관하게_NORMAL이다() {
		AnalysisResult result = analyzeProductAndCombination(EvidenceStrength.MEDIUM, EvidenceStrength.WEAK);

		assertThat(result.hold()).isFalse();
		assertThat(result.confidence()).isEqualTo(Confidence.NORMAL);
	}

	// ------------------------------------------------------------------
	// type priority (동점일 때 후보 목록 정렬 순서에서만 관측 가능 — HOLD 여부와는 별개)
	// ------------------------------------------------------------------

	@Test
	void 서로_다른_타입이_동점이면_후보_목록에서_type_우선순위가_앞선다() {
		CombinationCandidateInput combination = new CombinationCandidateInput("RETINOL", "ACID", ConflictPlacement.SPLIT_AM_PM);
		ObservationCandidateInput weather = new ObservationCandidateInput(3, 1, SUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(combination), null, weather);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.candidates().get(0).type()).isEqualTo(CandidateType.COMBINATION);
		assertThat(result.candidates().get(1).type()).isEqualTo(CandidateType.WEATHER);
	}

	// ------------------------------------------------------------------
	// candidate gate exclusion reason
	// ------------------------------------------------------------------

	@Test
	void product_대상이_없으면_NO_TARGET으로_제외된다() {
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null));

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.PRODUCT, ExclusionReason.NO_TARGET));
		assertThat(result.candidates()).noneMatch(c -> c.type() == CandidateType.PRODUCT);
	}

	@Test
	void combination_대상이_없으면_NO_TARGET으로_제외된다() {
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null));

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.COMBINATION, ExclusionReason.NO_TARGET));
	}

	@Test
	void sleep_대상이_없으면_NO_TARGET으로_제외된다() {
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null));

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.SLEEP, ExclusionReason.NO_TARGET));
	}

	@Test
	void weather_대상이_없으면_NO_TARGET으로_제외된다() {
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null));

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.WEATHER, ExclusionReason.NO_TARGET));
	}

	@Test
	void 모든_후보가_제외되면_HOLD이고_4개_타입_모두_제외_사유가_남는다() {
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null));

		assertThat(result.hold()).isTrue();
		assertThat(result.candidates()).isEmpty();
		assertThat(result.exclusions()).hasSize(4);
		assertThat(result.exclusions()).extracting(CandidateExclusion::type)
				.containsExactlyInAnyOrder(CandidateType.PRODUCT, CandidateType.COMBINATION, CandidateType.SLEEP, CandidateType.WEATHER);
		assertThat(result.exclusions()).allMatch(e -> e.reason() == ExclusionReason.NO_TARGET);
	}

	// ------------------------------------------------------------------
	// CandidateResult.coverageDays — SLEEP/WEATHER만 실제 값을 갖고 PRODUCT/COMBINATION은 항상 null
	// (2026-08-18 확정, Manyfast 통합 분석 F-ZSPZHH updateData [후보 수집] 규칙 재확인)
	// ------------------------------------------------------------------

	@Test
	void product_후보의_coverageDays는_null이다() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.minusDays(10), 1);

		assertThat(result.candidates().get(0).coverageDays()).isNull();
	}

	@Test
	void combination_후보의_coverageDays는_null이다() {
		AnalysisResult result = analyzeSingleCombination(ConflictPlacement.SAME_TIME_SLOT);

		assertThat(result.candidates().get(0).coverageDays()).isNull();
	}

	@Test
	void sleep_후보의_coverageDays가_보존된다() {
		RecordCoverage coverage = new RecordCoverage(ANALYSIS_DATE.minusDays(9));
		ObservationCandidateInput sleep = new ObservationCandidateInput(10, 7, coverage);

		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), sleep, null));

		assertThat(result.candidates().get(0).coverageDays()).isEqualTo(10L);
	}

	@Test
	void weather_후보의_coverageDays가_보존된다() {
		RecordCoverage coverage = new RecordCoverage(ANALYSIS_DATE.minusDays(9));
		ObservationCandidateInput weather = new ObservationCandidateInput(10, 7, coverage);

		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, weather));

		assertThat(result.candidates().get(0).coverageDays()).isEqualTo(10L);
	}
}
