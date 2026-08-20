package com.afterglow.domain.tracking.daily.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.afterglow.domain.tracking.daily.domain.DailyTracking;
import com.afterglow.domain.tracking.daily.domain.DailyTrackingRepository;

@Repository
public interface DailyTrackingJpaRepository
	extends JpaRepository<DailyTracking, Long>, DailyTrackingRepository {

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
