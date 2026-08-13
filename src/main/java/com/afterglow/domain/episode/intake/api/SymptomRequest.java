package com.afterglow.domain.episode.intake.api;

import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;

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
