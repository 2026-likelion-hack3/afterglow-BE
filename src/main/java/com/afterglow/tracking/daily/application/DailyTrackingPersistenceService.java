package com.afterglow.tracking.daily.application;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.tracking.daily.domain.ConditionLevel;
import com.afterglow.tracking.daily.domain.DailyTracking;
import com.afterglow.tracking.daily.domain.DailyTrackingRepository;
import com.afterglow.tracking.daily.domain.SleepLevel;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyTrackingPersistenceService {

	private final DailyTrackingRepository dailyTrackingRepository;

	@Transactional
	public void saveOrUpdate(
		Long accountId,
		LocalDate recordedDate,
		SleepLevel sleepLevel,
		ConditionLevel conditionLevel,
		Double temperature,
		Double humidity,
		Double uvIndex
	) {

		DailyTracking tracking = dailyTrackingRepository
			.findByAccountIdAndRecordedDate(accountId, recordedDate)
			.orElse(null);

		if (tracking == null) {
			tracking = DailyTracking.create(
				accountId,
				recordedDate,
				sleepLevel,
				conditionLevel,
				temperature,
				humidity,
				uvIndex
			);

			dailyTrackingRepository.save(tracking);
			return;
		}

		tracking.update(
			sleepLevel,
			conditionLevel,
			temperature,
			humidity,
			uvIndex
		);
	}
}
