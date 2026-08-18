package com.afterglow.vanity.application;

import java.time.LocalDate;
import java.util.Set;

import com.afterglow.vanity.InteractionTag;
import com.afterglow.vanity.UsageTiming;

public record VanityQueryResponse(
	Long productId,
	LocalDate openedAt,
	Set<InteractionTag> interactionTags,
	UsageTiming usageTiming
) {
}
