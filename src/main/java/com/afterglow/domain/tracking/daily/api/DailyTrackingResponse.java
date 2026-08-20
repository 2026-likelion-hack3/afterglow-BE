package com.afterglow.domain.tracking.daily.api;

import java.time.LocalDate;

import com.afterglow.domain.tracking.daily.domain.ConditionLevel;
import com.afterglow.domain.tracking.daily.domain.DailyTracking;
import com.afterglow.domain.tracking.daily.domain.SleepLevel;

public record DailyTrackingResponse(
	Long id,
	LocalDate recordedDate,
	SleepLevel sleepLevel,
	ConditionLevel conditionLevel,
	Double temperature,
	Double humidity,
	Double uvIndex
) {

	public DailyTrackingResponse(DailyTracking tracking) {
		this(
			tracking.getId(),
			tracking.getRecordedDate(),
			tracking.getSleepLevel(),
			tracking.getConditionLevel(),
			tracking.getTemperature(),
			tracking.getHumidity(),
			tracking.getUvIndex()
		);
	}
}
