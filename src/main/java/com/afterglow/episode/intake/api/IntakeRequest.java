package com.afterglow.episode.intake.api;

import java.util.Set;

import com.afterglow.episode.intake.domain.BodyPart;
import com.afterglow.episode.intake.domain.OnsetPeriod;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record IntakeRequest(
		@NotNull OnsetPeriod onsetPeriod,
		@NotEmpty Set<BodyPart> bodyParts,
		String recentNewProductName,
		String notes
) {
}
