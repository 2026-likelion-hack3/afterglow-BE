package com.afterglow.domain.episode.card.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.afterglow.domain.episode.analysis.domain.AnalysisResult;
import com.afterglow.domain.episode.analysis.domain.CandidateExclusion;
import com.afterglow.domain.episode.analysis.domain.CandidateResult;
import com.afterglow.domain.episode.analysis.domain.CandidateType;
import com.afterglow.domain.episode.analysis.domain.Confidence;
import com.afterglow.domain.episode.analysis.domain.EvidenceStrength;
import com.afterglow.domain.episode.analysis.domain.ExclusionReason;
import com.afterglow.domain.episode.analysis.domain.FrequencyEvidence;
import com.afterglow.domain.episode.analysis.domain.TimingEvidence;

class ResultCardAssemblerTest {

	private final ResultCardAssembler assembler = new ResultCardAssembler();

	@Test
	void 정상_판정_결과는_카드_3장을_만들고_첫_카드는_DISCONTINUE다() {
		CandidateResult top = productCandidate(EvidenceStrength.STRONG);
		AnalysisResult analysisResult = AnalysisResult.determined(List.of(top), List.of(), top, Confidence.HIGH);

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.hold()).isFalse();
		assertThat(result.cards()).hasSize(3);
		assertThat(result.cards().get(0).type()).isEqualTo(ResultCardType.DISCONTINUE);
		assertThat(result.cards().get(0).causeType()).isEqualTo(CandidateType.PRODUCT);
		// PRODUCT는 coverage 게이트가 없어 CandidateResult.coverageDays 자체가 항상 null이다(2026-08-18 확정) —
		// 카드가 임의 숫자를 만들어 채우지 않는지 여기서 함께 확인한다.
		assertThat(result.cards().get(0).coverageDays()).isNull();
	}

	@Test
	void HIGH_confidence는_그대로_노출된다() {
		CandidateResult top = productCandidate(EvidenceStrength.STRONG);
		AnalysisResult analysisResult = AnalysisResult.determined(List.of(top), List.of(), top, Confidence.HIGH);

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
		assertThat(result.holdReason()).isNull();
	}

	@Test
	void NORMAL_confidence는_그대로_노출된다() {
		CandidateResult top = productCandidate(EvidenceStrength.MEDIUM);
		AnalysisResult analysisResult = AnalysisResult.determined(List.of(top), List.of(), top, Confidence.NORMAL);

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.confidence()).isEqualTo(Confidence.NORMAL);
	}

	@Test
	void 원인이_WEATHER면_첫_카드가_DISCONTINUE_대신_DO_MORE_TODAY다() {
		CandidateResult top = new CandidateResult(
				CandidateType.WEATHER, EvidenceStrength.STRONG, new FrequencyEvidence(10, 8), 10L);
		AnalysisResult analysisResult = AnalysisResult.determined(List.of(top), List.of(), top, Confidence.HIGH);

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.cards().get(0).type()).isEqualTo(ResultCardType.DO_MORE_TODAY);
		assertThat(result.cards().get(0).causeType()).isEqualTo(CandidateType.WEATHER);
	}

	@Test
	void HOLD이면_첫_카드가_WITHHELD로_대체되고_confidence는_없다() {
		AnalysisResult analysisResult = AnalysisResult.hold(List.of(), List.of());

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.hold()).isTrue();
		assertThat(result.confidence()).isNull();
		assertThat(result.cards().get(0).type()).isEqualTo(ResultCardType.WITHHELD);
	}

	@Test
	void 전부_NO_TARGET으로_제외되면_보류_사유가_NO_TARGET이다() {
		List<CandidateExclusion> exclusions = List.of(
				new CandidateExclusion(CandidateType.PRODUCT, ExclusionReason.NO_TARGET),
				new CandidateExclusion(CandidateType.COMBINATION, ExclusionReason.NO_TARGET),
				new CandidateExclusion(CandidateType.SLEEP, ExclusionReason.NO_TARGET),
				new CandidateExclusion(CandidateType.WEATHER, ExclusionReason.NO_TARGET));
		AnalysisResult analysisResult = AnalysisResult.hold(List.of(), exclusions);

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.NO_TARGET);
	}

	@Test
	void 하나라도_INSUFFICIENT_RECORDS면_보류_사유가_INSUFFICIENT_RECORDS다() {
		List<CandidateExclusion> exclusions = List.of(
				new CandidateExclusion(CandidateType.PRODUCT, ExclusionReason.NO_TARGET),
				new CandidateExclusion(CandidateType.SLEEP, ExclusionReason.INSUFFICIENT_RECORDS));
		AnalysisResult analysisResult = AnalysisResult.hold(List.of(), exclusions);

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.INSUFFICIENT_RECORDS);
	}

	@Test
	void 후보는_있었지만_근거가_약해_보류면_INCONCLUSIVE_EVIDENCE다() {
		CandidateResult weakTop = productCandidate(EvidenceStrength.WEAK);
		AnalysisResult analysisResult = AnalysisResult.hold(List.of(weakTop), List.of());

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.INCONCLUSIVE_EVIDENCE);
	}

	@Test
	void 병원_카드는_determined_결과에서도_항상_포함된다() {
		CandidateResult top = productCandidate(EvidenceStrength.STRONG);
		AnalysisResult analysisResult = AnalysisResult.determined(List.of(top), List.of(), top, Confidence.HIGH);

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.cards().get(2).type()).isEqualTo(ResultCardType.HOSPITAL_VISIT);
	}

	@Test
	void 병원_카드는_HOLD_결과에서도_항상_포함된다() {
		AnalysisResult analysisResult = AnalysisResult.hold(List.of(), List.of());

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.cards().get(2).type()).isEqualTo(ResultCardType.HOSPITAL_VISIT);
	}

	@Test
	void 카드는_hold_여부와_무관하게_항상_3장이다() {
		CandidateResult top = productCandidate(EvidenceStrength.STRONG);
		AnalysisResult determined = AnalysisResult.determined(List.of(top), List.of(), top, Confidence.HIGH);
		AnalysisResult hold = AnalysisResult.hold(List.of(), List.of());

		assertThat(assembler.assemble(determined).cards()).hasSize(3);
		assertThat(assembler.assemble(hold).cards()).hasSize(3);
	}

	@Test
	void 두번째_카드는_CONTINUE_USE_타입으로_자리만_있고_근거는_없다() {
		CandidateResult top = productCandidate(EvidenceStrength.STRONG);
		AnalysisResult analysisResult = AnalysisResult.determined(List.of(top), List.of(), top, Confidence.HIGH);

		ResultCardResult result = assembler.assemble(analysisResult);

		ResultCard secondCard = result.cards().get(1);
		assertThat(secondCard.type()).isEqualTo(ResultCardType.CONTINUE_USE);
		assertThat(secondCard.causeType()).isNull();
		assertThat(secondCard.evidence()).isNull();
		assertThat(secondCard.coverageDays()).isNull();
	}

	@Test
	void sleep_weather처럼_coverageDays를_가진_후보는_카드에_그대로_노출된다() {
		// PRODUCT/COMBINATION은 CandidateResult.coverageDays가 항상 null이라(2026-08-18 확정) 이 통과
		// 동작은 SLEEP/WEATHER로만 검증할 수 있다.
		CandidateResult top = new CandidateResult(
				CandidateType.SLEEP, EvidenceStrength.STRONG, new FrequencyEvidence(10, 8), 13L);
		AnalysisResult analysisResult = AnalysisResult.determined(List.of(top), List.of(), top, Confidence.HIGH);

		ResultCardResult result = assembler.assemble(analysisResult);

		assertThat(result.cards().get(0).coverageDays()).isEqualTo(13L);
	}

	@Test
	void FrequencyEvidence의_관찰_횟수와_coverageDays는_서로_다른_값으로_섞이지_않는다() {
		FrequencyEvidence evidence = new FrequencyEvidence(10, 6);
		CandidateResult top = new CandidateResult(CandidateType.SLEEP, EvidenceStrength.STRONG, evidence, 9L);
		AnalysisResult analysisResult = AnalysisResult.determined(List.of(top), List.of(), top, Confidence.HIGH);

		ResultCardResult result = assembler.assemble(analysisResult);

		ResultCard firstCard = result.cards().get(0);
		FrequencyEvidence cardEvidence = (FrequencyEvidence) firstCard.evidence();
		assertThat(cardEvidence.observationCount()).isEqualTo(10);
		assertThat(cardEvidence.matchedObservationCount()).isEqualTo(6);
		assertThat(firstCard.coverageDays()).isEqualTo(9L);
	}

	/** PRODUCT는 coverage 게이트가 없어 CandidateResult.coverageDays가 항상 null이다(2026-08-18 확정). */
	private CandidateResult productCandidate(EvidenceStrength strength) {
		return new CandidateResult(CandidateType.PRODUCT, strength,
				new TimingEvidence(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10)), null);
	}
}
