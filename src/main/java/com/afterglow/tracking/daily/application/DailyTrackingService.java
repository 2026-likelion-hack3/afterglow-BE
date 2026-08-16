package com.afterglow.tracking.daily.application;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.account.domain.Account;
import com.afterglow.account.domain.AccountRepository;
import com.afterglow.tracking.daily.api.DailyTrackingResponse;
import com.afterglow.tracking.daily.domain.ConditionLevel;
import com.afterglow.tracking.daily.domain.DailyTracking;
import com.afterglow.tracking.daily.domain.DailyTrackingRepository;
import com.afterglow.tracking.daily.domain.SleepLevel;
import com.afterglow.tracking.daily.infrastructure.WeatherClient;
import com.afterglow.tracking.daily.infrastructure.WeatherData;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyTrackingService {

	private final DailyTrackingRepository dailyTrackingRepository;
	private final AccountRepository accountRepository;
	private final WeatherClient weatherClient;

	@Transactional
	public void createOrUpdate(
		Long accountId,
		LocalDate recordedDate,
		SleepLevel sleepLevel,
		ConditionLevel conditionLevel,
		double latitude,
		double longitude) {

		Account account = accountRepository.findById(accountId)
			.orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

		WeatherData weather = weatherClient.getCurrentWeather(latitude, longitude);

		DailyTracking tracking = dailyTrackingRepository
			.findByAccountIdAndRecordedDate(accountId, recordedDate)
			.orElse(null);

		if (tracking == null) {
			tracking = DailyTracking.create(
				account,
				recordedDate,
				sleepLevel,
				conditionLevel,
				weather.temperature(),
				weather.humidity(),
				weather.uvIndex()
			);

			dailyTrackingRepository.save(tracking);
			return;
		}

		tracking.update(
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
}
