package com.afterglow.tracking.daily.application;

import java.time.LocalDate;

import com.afterglow.tracking.daily.domain.SleepLevel;

public record TrackingQueryResponse(
	LocalDate recordedDate,
	SleepLevel sleepLevel,
	Double temperature,
	Double humidity,
	Double uvIndex
) {
}
