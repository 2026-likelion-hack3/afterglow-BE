package com.afterglow.domain.tracking.daily.infrastructure;

public record WeatherData(
	Double temperature,
	Double minTemperature,
	Double humidity,
	Double uvIndex
) {
}
