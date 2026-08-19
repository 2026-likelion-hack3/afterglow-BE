package com.afterglow.domain.tracking.daily.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.tracking.daily.domain.DailyTrackingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingQueryService {

	private final DailyTrackingRepository dailyTrackingRepository;

	public List<TrackingQueryResponse> findByPeriod(
		Long accountId,
		LocalDate from,
		LocalDate to
	) {
		return dailyTrackingRepository
			.findByAccountIdAndRecordedDateBetween(accountId, from, to)
			.stream()
			.map(tracking -> new TrackingQueryResponse(
				tracking.getRecordedDate(),
				tracking.getSleepLevel(),
				tracking.getTemperature(),
				tracking.getMinTemperature(),
				tracking.getHumidity(),
				tracking.getUvIndex()
			))
			.toList();
	}
}
