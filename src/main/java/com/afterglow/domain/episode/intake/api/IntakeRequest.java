package com.afterglow.domain.episode.intake.api;

import java.util.Set;

import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.OnsetPeriod;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record IntakeRequest(
		@NotNull OnsetPeriod onsetPeriod,
		@NotEmpty Set<BodyPart> bodyParts,
		String recentNewProductName,
		String notes
) {
}
