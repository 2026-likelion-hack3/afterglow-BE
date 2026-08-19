package com.afterglow.domain.episode.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.afterglow.domain.episode.card.domain.ResultCard;
import com.afterglow.domain.episode.card.domain.ResultCardHoldReason;
import com.afterglow.domain.episode.card.domain.ResultCardResult;
import com.afterglow.domain.episode.card.domain.ResultCardType;

class AnalysisExplanationFallbackTest {

	private final AnalysisExplanationFallback fallback = new AnalysisExplanationFallback();

	@Test
	void hold이면_headline과_summary에서_원인을_확정하지_않는다() {
		ResultCardResult hold = ResultCardResult.hold(ResultCardHoldReason.INCONCLUSIVE_EVIDENCE, List.of(
				new ResultCard(ResultCardType.WITHHELD, null, null, null),
				new ResultCard(ResultCardType.CONTINUE_USE, null, null, null),
				new ResultCard(ResultCardType.HOSPITAL_VISIT, null, null, null)
		));

		AnalysisExplanation explanation = fallback.explain(hold);

		assertThat(explanation.headline()).contains("어려워요");
		assertThat(explanation.summary()).doesNotContain("확정했");
		assertThat(explanation.evidenceNotes()).isEmpty();
		assertThat(explanation.nextAction()).isNotBlank();
	}

	@Test
	void hold_사유가_NO_TARGET이면_기록_약속을_하지_않는다() {
		ResultCardResult hold = ResultCardResult.hold(ResultCardHoldReason.NO_TARGET, List.of(
				new ResultCard(ResultCardType.WITHHELD, null, null, null),
				new ResultCard(ResultCardType.CONTINUE_USE, null, null, null),
				new ResultCard(ResultCardType.HOSPITAL_VISIT, null, null, null)
		));

		AnalysisExplanation explanation = fallback.explain(hold);

		assertThat(explanation.summary()).contains("후보 자체가 없");
	}

	@Test
	void hold이_아니고_PRODUCT_원인이면_사용_시작일_증상_시작일을_evidenceNotes에_담는다() {
		TimingEvidence evidence = new TimingEvidence(100L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5));
		ResultCard discontinueCard = new ResultCard(ResultCardType.DISCONTINUE, CandidateType.PRODUCT, evidence, null);
		ResultCardResult determined = ResultCardResult.determined(Confidence.HIGH, List.of(
				discontinueCard,
				new ResultCard(ResultCardType.CONTINUE_USE, null, null, null),
				new ResultCard(ResultCardType.HOSPITAL_VISIT, null, null, null)
		));

		AnalysisExplanation explanation = fallback.explain(determined);

		assertThat(explanation.evidenceNotes()).hasSize(1);
		assertThat(explanation.evidenceNotes().get(0)).contains("2026-08-01").contains("2026-08-05");
		assertThat(explanation.nextAction()).contains("쉬어보는");
	}

	@Test
	void confidence가_NORMAL이면_기록이_더_모이면_확실해진다는_문구가_붙는다() {
		FrequencyEvidence evidence = new FrequencyEvidence(5, 4);
		ResultCard doMoreCard = new ResultCard(ResultCardType.DO_MORE_TODAY, CandidateType.WEATHER, evidence, 10L);
		ResultCardResult determined = ResultCardResult.determined(Confidence.NORMAL, List.of(
				doMoreCard,
				new ResultCard(ResultCardType.CONTINUE_USE, null, null, null),
				new ResultCard(ResultCardType.HOSPITAL_VISIT, null, null, null)
		));

		AnalysisExplanation explanation = fallback.explain(determined);

		assertThat(explanation.summary()).contains("더 모이면");
		assertThat(explanation.headline()).contains("날씨");
	}

	@Test
	void confidence가_HIGH이면_기록이_더_모이면_확실해진다는_문구가_붙지_않는다() {
		FrequencyEvidence evidence = new FrequencyEvidence(5, 5);
		ResultCard doMoreCard = new ResultCard(ResultCardType.DO_MORE_TODAY, CandidateType.WEATHER, evidence, 10L);
		ResultCardResult determined = ResultCardResult.determined(Confidence.HIGH, List.of(
				doMoreCard,
				new ResultCard(ResultCardType.CONTINUE_USE, null, null, null),
				new ResultCard(ResultCardType.HOSPITAL_VISIT, null, null, null)
		));

		AnalysisExplanation explanation = fallback.explain(determined);

		assertThat(explanation.summary()).doesNotContain("더 모이면");
	}

	@Test
	void COMBINATION_원인이면_충돌_태그_쌍을_evidenceNotes에_담는다() {
		CombinationEvidence evidence = new CombinationEvidence("RETINOL", "ACID", ConflictPlacement.SAME_TIME_SLOT);
		ResultCard discontinueCard = new ResultCard(ResultCardType.DISCONTINUE, CandidateType.COMBINATION, evidence, null);
		ResultCardResult determined = ResultCardResult.determined(Confidence.HIGH, List.of(
				discontinueCard,
				new ResultCard(ResultCardType.CONTINUE_USE, null, null, null),
				new ResultCard(ResultCardType.HOSPITAL_VISIT, null, null, null)
		));

		AnalysisExplanation explanation = fallback.explain(determined);

		assertThat(explanation.evidenceNotes().get(0)).contains("RETINOL").contains("ACID");
	}
}
