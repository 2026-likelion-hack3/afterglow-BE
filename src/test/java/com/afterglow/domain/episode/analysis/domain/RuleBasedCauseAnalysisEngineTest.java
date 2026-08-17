package com.afterglow.domain.episode.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class RuleBasedCauseAnalysisEngineTest {

	private static final LocalDate ANALYSIS_DATE = LocalDate.of(2026, 8, 16);
	private static final LocalDate SYMPTOM_START = LocalDate.of(2026, 8, 16);

	/** analysisDate 기준 정확히 7 calendar days 커버리지(게이트를 항상 통과) — 규칙 자체를 검증하는 테스트에서 게이트는 관심사가 아니므로 고정값을 쓴다. */
	private static final RecordCoverage SUFFICIENT_COVERAGE = new RecordCoverage(ANALYSIS_DATE.minusDays(6));
	private static final RecordCoverage INSUFFICIENT_COVERAGE = new RecordCoverage(ANALYSIS_DATE.minusDays(5));

	private final RuleBasedCauseAnalysisEngine engine = new RuleBasedCauseAnalysisEngine();

	// ------------------------------------------------------------------
	// 공통 7-day gate
	// ------------------------------------------------------------------

	@Test
	void 대상이_없으면_해당_타입_후보가_생성되지_않는다() {
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.candidates()).isEmpty();
		assertThat(result.hold()).isTrue();
	}

	@Test
	void 대상은_있지만_커버리지가_6일이면_후보에서_제외된다() {
		ProductCandidateInput product = new ProductCandidateInput(
				1L, SYMPTOM_START.minusDays(5), SYMPTOM_START, 1, INSUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(product), List.of(), null, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.candidates()).isEmpty();
		assertThat(result.hold()).isTrue();
	}

	@Test
	void 커버리지가_정확히_7일이면_후보로_채택된다() {
		ProductCandidateInput product = new ProductCandidateInput(
				1L, SYMPTOM_START.minusDays(5), SYMPTOM_START, 1, SUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(product), List.of(), null, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.candidates()).hasSize(1);
	}

	@Test
	void 커버리지가_7일보다_많으면_후보로_채택된다() {
		ProductCandidateInput product = new ProductCandidateInput(
				1L, SYMPTOM_START.minusDays(5), SYMPTOM_START, 1, new RecordCoverage(ANALYSIS_DATE.minusDays(10)));
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(product), List.of(), null, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.candidates()).hasSize(1);
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
				1L, usageStartDate, SYMPTOM_START, changedProductCountInWindow, SUFFICIENT_COVERAGE);
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
		CombinationCandidateInput combination = new CombinationCandidateInput("RETINOL", "ACID", placement, SUFFICIENT_COVERAGE);
		return engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(combination), null, null));
	}

	// ------------------------------------------------------------------
	// 수면 / 날씨
	// ------------------------------------------------------------------

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
	// ranking / gap
	// ------------------------------------------------------------------

	@Test
	void 서로_다른_타입이_3점으로_동점이면_gap이_0이라_HOLD이다() {
		// PRODUCT(STRONG=3) vs COMBINATION(STRONG=3), 서로 다른 타입 — same-type tie 규칙이 아니라
		// gap<0.2 규칙으로 HOLD가 되는지를 검증한다.
		AnalysisResult result = analyzeProductAndCombination(EvidenceStrength.STRONG, EvidenceStrength.STRONG);

		assertThat(result.hold()).isTrue();
	}

	@Test
	void 점수가_3대2면_gap은_0_333이고_HOLD가_아니며_NORMAL이다() {
		AnalysisResult result = analyzeProductAndCombination(EvidenceStrength.STRONG, EvidenceStrength.MEDIUM);

		assertThat(result.hold()).isFalse();
		assertThat(result.confidence()).isEqualTo(Confidence.NORMAL);
	}

	@Test
	void 점수가_3대1이면_gap은_0_667이고_HIGH이다() {
		AnalysisResult result = analyzeProductAndCombination(EvidenceStrength.STRONG, EvidenceStrength.WEAK);

		assertThat(result.hold()).isFalse();
		assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
	}

	@Test
	void 점수가_2대1이면_gap은_0_5이고_NORMAL이다() {
		AnalysisResult result = analyzeProductAndCombination(EvidenceStrength.MEDIUM, EvidenceStrength.WEAK);

		assertThat(result.hold()).isFalse();
		assertThat(result.confidence()).isEqualTo(Confidence.NORMAL);
	}

	@Test
	void candidate가_1개면_gap은_1이고_STRONG이면_HIGH이다() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.minusDays(10), 1);

		assertThat(result.candidates()).hasSize(1);
		assertThat(result.hold()).isFalse();
		assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
	}

	/** WEAK를 만들기 위해 timing이 어긋난 제품(WEAK) / 충돌 없는 조합(WEAK)을 각각 원하는 강도로 만들어 조합한다. */
	private AnalysisResult analyzeProductAndCombination(EvidenceStrength productStrength, EvidenceStrength combinationStrength) {
		ProductCandidateInput product = toProductWithStrength(productStrength);
		CombinationCandidateInput combination = toCombinationWithStrength(combinationStrength);
		return engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(product), List.of(combination), null, null));
	}

	private ProductCandidateInput toProductWithStrength(EvidenceStrength strength) {
		return switch (strength) {
			case STRONG -> new ProductCandidateInput(1L, SYMPTOM_START.minusDays(10), SYMPTOM_START, 1, SUFFICIENT_COVERAGE);
			case MEDIUM -> new ProductCandidateInput(1L, SYMPTOM_START.minusDays(10), SYMPTOM_START, 2, SUFFICIENT_COVERAGE);
			case WEAK -> new ProductCandidateInput(1L, SYMPTOM_START.plusDays(1), SYMPTOM_START, 1, SUFFICIENT_COVERAGE);
		};
	}

	private CombinationCandidateInput toCombinationWithStrength(EvidenceStrength strength) {
		ConflictPlacement placement = switch (strength) {
			case STRONG -> ConflictPlacement.SAME_TIME_SLOT;
			case MEDIUM -> ConflictPlacement.SPLIT_AM_PM;
			case WEAK -> ConflictPlacement.NONE;
		};
		return new CombinationCandidateInput("RETINOL", "ACID", placement, SUFFICIENT_COVERAGE);
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

	@Test
	void 같은_타입_안에서_top_후보가_동점이면_priority로_해결하지_않고_HOLD이며_topCandidate가_없다() {
		ProductCandidateInput productA = new ProductCandidateInput(1L, SYMPTOM_START.minusDays(10), SYMPTOM_START, 1, SUFFICIENT_COVERAGE);
		ProductCandidateInput productB = new ProductCandidateInput(2L, SYMPTOM_START.minusDays(5), SYMPTOM_START, 1, SUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(productA, productB), List.of(), null, null);

		AnalysisResult result = engine.analyze(input);

		// candidates 목록 자체는 정렬 결과로 두 후보를 모두 담고 있지만(둘 다 STRONG),
		// 이는 "1등이 정해졌다"는 뜻이 아니다 — topCandidate가 null인 것으로 그 사실을 확인한다.
		assertThat(result.candidates()).extracting(CandidateResult::strength)
				.containsExactly(EvidenceStrength.STRONG, EvidenceStrength.STRONG);
		assertThat(result.hold()).isTrue();
		assertThat(result.topCandidate()).isNull();
		assertThat(result.confidence()).isNull();
	}

	@Test
	void gap이_0_2_미만이면_HOLD이고_confidence가_없다() {
		// 정수 점수(1/2/3) 체계에서 0보다 크고 0.2 미만인 gap은 나올 수 없다 — 동점(gap=0)일 때만 이 조건에
		// 걸린다. 서로 다른 타입의 MEDIUM 동점(2 vs 2)으로 gap=0 케이스를 검증한다.
		CombinationCandidateInput combination = new CombinationCandidateInput("RETINOL", "ACID", ConflictPlacement.SPLIT_AM_PM, SUFFICIENT_COVERAGE);
		ObservationCandidateInput sleep = new ObservationCandidateInput(3, 1, SUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(combination), sleep, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.candidates()).extracting(CandidateResult::strength)
				.containsExactly(EvidenceStrength.MEDIUM, EvidenceStrength.MEDIUM);
		assertThat(result.hold()).isTrue();
		assertThat(result.topCandidate()).isNull();
		assertThat(result.confidence()).isNull();
	}

	// ------------------------------------------------------------------
	// type priority (동점일 때 후보 목록 정렬 순서에서만 관측 가능 — HOLD 여부와는 별개)
	// ------------------------------------------------------------------

	@Test
	void 서로_다른_타입이_동점이면_후보_목록에서_type_우선순위가_앞선다() {
		CombinationCandidateInput combination = new CombinationCandidateInput("RETINOL", "ACID", ConflictPlacement.SPLIT_AM_PM, SUFFICIENT_COVERAGE);
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
	void product_coverage가_부족하면_INSUFFICIENT_RECORDS로_제외된다() {
		ProductCandidateInput product = new ProductCandidateInput(
				1L, SYMPTOM_START.minusDays(10), SYMPTOM_START, 1, INSUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(product), List.of(), null, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.exclusions())
				.contains(new CandidateExclusion(CandidateType.PRODUCT, ExclusionReason.INSUFFICIENT_RECORDS, "1"));
		assertThat(result.candidates()).noneMatch(c -> c.type() == CandidateType.PRODUCT);
	}

	@Test
	void combination_대상이_없으면_NO_TARGET으로_제외된다() {
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null));

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.COMBINATION, ExclusionReason.NO_TARGET));
	}

	@Test
	void combination_coverage가_부족하면_INSUFFICIENT_RECORDS로_제외된다() {
		CombinationCandidateInput combination = new CombinationCandidateInput(
				"RETINOL", "ACID", ConflictPlacement.SAME_TIME_SLOT, INSUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(combination), null, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.exclusions())
				.contains(new CandidateExclusion(CandidateType.COMBINATION, ExclusionReason.INSUFFICIENT_RECORDS, "RETINOLxACID"));
		assertThat(result.candidates()).noneMatch(c -> c.type() == CandidateType.COMBINATION);
	}

	@Test
	void sleep_대상이_없으면_NO_TARGET으로_제외된다() {
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null));

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.SLEEP, ExclusionReason.NO_TARGET));
	}

	@Test
	void sleep_coverage가_부족하면_INSUFFICIENT_RECORDS로_제외된다() {
		ObservationCandidateInput sleep = new ObservationCandidateInput(10, 7, INSUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), sleep, null);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.SLEEP, ExclusionReason.INSUFFICIENT_RECORDS));
		assertThat(result.candidates()).noneMatch(c -> c.type() == CandidateType.SLEEP);
	}

	@Test
	void weather_대상이_없으면_NO_TARGET으로_제외된다() {
		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, null));

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.WEATHER, ExclusionReason.NO_TARGET));
	}

	@Test
	void weather_coverage가_부족하면_INSUFFICIENT_RECORDS로_제외된다() {
		ObservationCandidateInput weather = new ObservationCandidateInput(10, 7, INSUFFICIENT_COVERAGE);
		AnalysisInput input = new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(), null, weather);

		AnalysisResult result = engine.analyze(input);

		assertThat(result.exclusions()).contains(new CandidateExclusion(CandidateType.WEATHER, ExclusionReason.INSUFFICIENT_RECORDS));
		assertThat(result.candidates()).noneMatch(c -> c.type() == CandidateType.WEATHER);
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
	// CandidateResult.coverageDays (Result Card용, #41) — 기존 7-day gate/strength/ranking 판정에는
	// 영향을 주지 않는 추가 필드다.
	// ------------------------------------------------------------------

	@Test
	void product_후보의_coverageDays가_보존된다() {
		RecordCoverage coverage = new RecordCoverage(ANALYSIS_DATE.minusDays(9));
		ProductCandidateInput product = new ProductCandidateInput(1L, SYMPTOM_START.minusDays(10), SYMPTOM_START, 1, coverage);

		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(product), List.of(), null, null));

		assertThat(result.candidates().get(0).coverageDays()).isEqualTo(10L);
	}

	@Test
	void combination_후보의_coverageDays가_보존된다() {
		RecordCoverage coverage = new RecordCoverage(ANALYSIS_DATE.minusDays(9));
		CombinationCandidateInput combination = new CombinationCandidateInput("RETINOL", "ACID", ConflictPlacement.SAME_TIME_SLOT, coverage);

		AnalysisResult result = engine.analyze(new AnalysisInput(ANALYSIS_DATE, List.of(), List.of(combination), null, null));

		assertThat(result.candidates().get(0).coverageDays()).isEqualTo(10L);
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

	@Test
	void coverageDays_추가는_기존_strength_판정에_영향을_주지_않는다() {
		AnalysisResult result = analyzeSingleProduct(SYMPTOM_START.minusDays(10), 1);

		assertThat(result.candidates().get(0).strength()).isEqualTo(EvidenceStrength.STRONG);
		assertThat(result.candidates().get(0).coverageDays()).isEqualTo(7L);
	}
}
