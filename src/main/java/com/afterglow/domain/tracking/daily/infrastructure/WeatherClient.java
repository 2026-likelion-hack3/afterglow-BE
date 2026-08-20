package com.afterglow.domain.tracking.daily.infrastructure;

import java.time.LocalDate;

public interface WeatherClient {

	WeatherData getWeatherByDate(
		double latitude,
		double longitude,
		LocalDate date
	);
}
