package com.afterglow.tracking.daily.domain;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyTrackingRepository {

	DailyTracking save(DailyTracking dailyTracking);

	Optional<DailyTracking> findByAccountIdAndRecordedDate(
		Long accountId,
		LocalDate recordedDate
	);
}
