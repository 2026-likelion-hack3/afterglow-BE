package com.afterglow.domain.tracking.daily.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyTrackingRepository {

	DailyTracking save(DailyTracking dailyTracking);

	Optional<DailyTracking> findByAccountIdAndRecordedDate(
		Long accountId,
		LocalDate recordedDate
	);

	List<DailyTracking> findByAccountIdAndRecordedDateBetween(
		Long accountId,
		LocalDate from,
		LocalDate to
	);
}
