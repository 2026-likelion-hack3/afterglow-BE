package com.afterglow.tracking.daily.api;

import java.time.LocalDate;

import com.afterglow.tracking.daily.domain.ConditionLevel;
import com.afterglow.tracking.daily.domain.SleepLevel;

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
