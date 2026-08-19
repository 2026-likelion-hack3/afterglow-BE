package com.afterglow.domain.vanity.application;

import java.time.LocalDate;
import java.util.Set;

import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.UsageTiming;

public record VanityQueryResponse(
	Long productId,
	LocalDate openedAt,
	Set<InteractionTag> interactionTags,
	UsageTiming usageTiming
) {
}
