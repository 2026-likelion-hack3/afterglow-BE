package com.afterglow.episode.intake.api;

import com.afterglow.episode.intake.domain.PrimarySymptom;
import com.afterglow.episode.intake.domain.Severity;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SymptomRequest(
		@NotNull @DecimalMin("0") @DecimalMax("360") Double angle,
		@NotNull @DecimalMin("0") @DecimalMax("1") Double radius,
		@NotNull PrimarySymptom primarySymptom,
		@NotNull Severity severity
) {
}
