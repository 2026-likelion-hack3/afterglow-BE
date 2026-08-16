package com.afterglow.tracking.daily.infrastructure;

public interface WeatherClient {

	WeatherData getCurrentWeather(double latitude, double longitude);
}
