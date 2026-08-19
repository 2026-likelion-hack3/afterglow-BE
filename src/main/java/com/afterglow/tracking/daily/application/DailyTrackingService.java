package com.afterglow.tracking.daily.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.tracking.daily.api.DailyTrackingResponse;
import com.afterglow.tracking.daily.domain.ConditionLevel;
import com.afterglow.tracking.daily.domain.SleepLevel;
import com.afterglow.tracking.daily.domain.DailyTrackingRepository;
import com.afterglow.tracking.daily.infrastructure.WeatherClient;
import com.afterglow.tracking.daily.infrastructure.WeatherData;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyTrackingService {

	private final DailyTrackingRepository dailyTrackingRepository;
	private final DailyTrackingPersistenceService dailyTrackingPersistenceService;
	private final WeatherClient weatherClient;

	public void createOrUpdate(
		Long accountId,
		LocalDate recordedDate,
		SleepLevel sleepLevel,
		ConditionLevel conditionLevel,
		double latitude,
		double longitude) {

		// 외부 Weather API 호출은 DB Transaction 밖에서 수행
		WeatherData weather = weatherClient.getWeatherByDate(
			latitude,
			longitude,
			recordedDate
		);

		// 날씨 조회가 끝난 후 DB 저장 Transaction 시작
		dailyTrackingPersistenceService.saveOrUpdate(
			accountId,
			recordedDate,
			sleepLevel,
			conditionLevel,
			weather.temperature(),
			weather.humidity(),
			weather.uvIndex()
		);
	}

	public DailyTrackingResponse getDailyTracking(
		Long accountId,
		LocalDate recordedDate) {

		return dailyTrackingRepository
			.findByAccountIdAndRecordedDate(accountId, recordedDate)
			.map(DailyTrackingResponse::new)
			.orElse(null);
	}

	public List<DailyTrackingResponse> getDailyTrackings(
		Long accountId,
		LocalDate from,
		LocalDate to) {

		return dailyTrackingRepository
			.findByAccountIdAndRecordedDateBetween(accountId, from, to)
			.stream()
			.map(DailyTrackingResponse::new)
			.toList();
	}
}
