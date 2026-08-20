package com.afterglow.domain.tracking.daily.api;

import java.time.LocalDate;

import com.afterglow.domain.tracking.daily.domain.ConditionLevel;
import com.afterglow.domain.tracking.daily.domain.SleepLevel;

import jakarta.validation.constraints.NotNull;

public record DailyTrackingRequest(
	LocalDate recordedDate,

	@NotNull
	SleepLevel sleepLevel,

	@NotNull
	ConditionLevel conditionLevel,

	@NotNull
	Double latitude,

	@NotNull
	Double longitude
) {
}
