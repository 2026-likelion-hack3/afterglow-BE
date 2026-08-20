package com.afterglow.domain.episode.analysis.application;

import java.time.LocalDate;
import java.util.Set;

import com.afterglow.domain.episode.analysis.domain.ReferenceCertainty;
import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.UsageTiming;
import com.afterglow.domain.vanity.application.VanityQueryResponse;

/**
 * Vanity {@link VanityQueryResponse}에 Analysis 기준일 계산 결과(referenceDate/certainty)를 더한 값 —
 * {@link EpisodeAnalysisService}만 쓰는 application 레이어 전용 타입이라 {@code analysis.domain}에 두지
 * 않는다(엔진이 Vanity concrete 타입을 모르게 하는 기존 관례 유지). 순수 함수라 Spring 없이 단위 테스트할
 * 수 있다.
 *
 * @param referenceDate OpeningPeriod → 기준일 환산(2026-08-20 확정): 최근=-14일, 1~3개월=-60일,
 *                       6개월 이상=-180일, 없으면 제품 등록일(createdAt)로 대체.
 * @param certainty     환산값이면 {@link ReferenceCertainty#ESTIMATED}, 등록일 대체면 {@link ReferenceCertainty#FALLBACK}.
 */
public record VanityProductReference(
		Long productId,
		LocalDate referenceDate,
		ReferenceCertainty certainty,
		Set<InteractionTag> interactionTags,
		UsageTiming usageTiming,
		LocalDate usageTimingChangedAt
) {
	public static VanityProductReference of(VanityQueryResponse product, LocalDate analysisDate) {
		LocalDate referenceDate = referenceDateOf(product, analysisDate);
		ReferenceCertainty certainty = product.openingPeriod() == null
				? ReferenceCertainty.FALLBACK : ReferenceCertainty.ESTIMATED;
		return new VanityProductReference(
				product.productId(), referenceDate, certainty,
				product.interactionTags(), product.usageTiming(), product.usageTimingChangedAt());
	}

	private static LocalDate referenceDateOf(VanityQueryResponse product, LocalDate analysisDate) {
		if (product.openingPeriod() == null) {
			return product.createdAt().toLocalDate();
		}
		return switch (product.openingPeriod()) {
			case RECENT -> analysisDate.minusDays(14);
			case ONE_TO_THREE_MONTHS -> analysisDate.minusDays(60);
			case SIX_MONTHS_OR_MORE -> analysisDate.minusDays(180);
		};
	}
}
