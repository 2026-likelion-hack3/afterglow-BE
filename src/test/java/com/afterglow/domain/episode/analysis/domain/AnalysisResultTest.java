package com.afterglow.domain.episode.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class AnalysisResultTest {

	private static final CandidateResult PRODUCT_STRONG =
			new CandidateResult(CandidateType.PRODUCT, EvidenceStrength.STRONG,
					new TimingEvidence(1L, java.time.LocalDate.of(2026, 8, 1), java.time.LocalDate.of(2026, 8, 10)));

	@Test
	void hold_결과는_topCandidate와_confidence가_항상_없다() {
		AnalysisResult result = AnalysisResult.hold(List.of(PRODUCT_STRONG), List.of());

		assertThat(result.hold()).isTrue();
		assertThat(result.topCandidate()).isNull();
		assertThat(result.confidence()).isNull();
	}

	@Test
	void determined_결과는_topCandidate와_confidence가_항상_있다() {
		AnalysisResult result = AnalysisResult.determined(List.of(PRODUCT_STRONG), List.of(), PRODUCT_STRONG, Confidence.HIGH);

		assertThat(result.hold()).isFalse();
		assertThat(result.topCandidate()).isEqualTo(PRODUCT_STRONG);
		assertThat(result.confidence()).isEqualTo(Confidence.HIGH);
	}

	@Test
	void hold이면서_confidence를_가진_상태는_생성_자체가_불가능하다() {
		assertThatThrownBy(() ->
				new AnalysisResult(List.of(PRODUCT_STRONG), List.of(), true, PRODUCT_STRONG, Confidence.NORMAL))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void hold가_아닌데_confidence가_없는_상태는_생성_자체가_불가능하다() {
		assertThatThrownBy(() ->
				new AnalysisResult(List.of(PRODUCT_STRONG), List.of(), false, PRODUCT_STRONG, null))
				.isInstanceOf(IllegalStateException.class);
	}
}
