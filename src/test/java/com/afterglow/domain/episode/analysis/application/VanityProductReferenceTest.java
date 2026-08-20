package com.afterglow.domain.episode.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.afterglow.domain.episode.analysis.domain.ReferenceCertainty;
import com.afterglow.domain.vanity.OpeningPeriod;
import com.afterglow.domain.vanity.UsageTiming;
import com.afterglow.domain.vanity.application.VanityQueryResponse;

class VanityProductReferenceTest {

	private static final LocalDate ANALYSIS_DATE = LocalDate.of(2026, 8, 20);

	@Test
	void RECENT은_분석일_14일_전이고_ESTIMATED다() {
		VanityProductReference reference = VanityProductReference.of(product(OpeningPeriod.RECENT), ANALYSIS_DATE);

		assertThat(reference.referenceDate()).isEqualTo(ANALYSIS_DATE.minusDays(14));
		assertThat(reference.certainty()).isEqualTo(ReferenceCertainty.ESTIMATED);
	}

	@Test
	void ONE_TO_THREE_MONTHS은_분석일_60일_전이다() {
		VanityProductReference reference = VanityProductReference.of(product(OpeningPeriod.ONE_TO_THREE_MONTHS), ANALYSIS_DATE);

		assertThat(reference.referenceDate()).isEqualTo(ANALYSIS_DATE.minusDays(60));
		assertThat(reference.certainty()).isEqualTo(ReferenceCertainty.ESTIMATED);
	}

	@Test
	void SIX_MONTHS_OR_MORE는_분석일_180일_전이다() {
		VanityProductReference reference = VanityProductReference.of(product(OpeningPeriod.SIX_MONTHS_OR_MORE), ANALYSIS_DATE);

		assertThat(reference.referenceDate()).isEqualTo(ANALYSIS_DATE.minusDays(180));
		assertThat(reference.certainty()).isEqualTo(ReferenceCertainty.ESTIMATED);
	}

	@Test
	void openingPeriod가_없으면_등록일로_대체되고_FALLBACK이다() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 12, 0);
		VanityQueryResponse product = new VanityQueryResponse(
				1L, null, null, createdAt, Set.of(), UsageTiming.MORNING, null);

		VanityProductReference reference = VanityProductReference.of(product, ANALYSIS_DATE);

		assertThat(reference.referenceDate()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(reference.certainty()).isEqualTo(ReferenceCertainty.FALLBACK);
	}

	private VanityQueryResponse product(OpeningPeriod openingPeriod) {
		return new VanityQueryResponse(
				1L, null, openingPeriod, LocalDateTime.of(2026, 1, 1, 0, 0), Set.of(), UsageTiming.MORNING, null);
	}
}
