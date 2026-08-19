package com.afterglow.domain.tracking.daily.application;

import java.time.LocalDate;

import com.afterglow.domain.tracking.daily.domain.SleepLevel;

public record TrackingQueryResponse(
	LocalDate recordedDate,
	SleepLevel sleepLevel,
	Double temperature,
	Double humidity,
	Double uvIndex
) {
}
