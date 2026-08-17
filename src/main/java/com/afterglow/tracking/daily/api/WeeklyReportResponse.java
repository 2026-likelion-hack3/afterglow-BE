package com.afterglow.tracking.daily.api;

import java.time.LocalDate;
import java.util.List;

public record WeeklyReportResponse(
	LocalDate from,
	LocalDate to,
	int recordedDays,
	int totalDays,
	List<DailyTrackingResponse> records
) {
}
