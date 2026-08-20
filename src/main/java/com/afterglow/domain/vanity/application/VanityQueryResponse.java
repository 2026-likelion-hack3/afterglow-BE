package com.afterglow.domain.vanity.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.OpeningPeriod;
import com.afterglow.domain.vanity.UsageTiming;

public record VanityQueryResponse(
	Long productId,
	LocalDate openedAt,
	OpeningPeriod openingPeriod,
	LocalDateTime createdAt,
	Set<InteractionTag> interactionTags,
	UsageTiming usageTiming,
	LocalDate usageTimingChangedAt
) {
}
