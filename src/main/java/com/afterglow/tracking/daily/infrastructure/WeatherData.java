package com.afterglow.tracking.daily.infrastructure;

public record WeatherData(
	Double temperature,
	Double humidity,
	Double uvIndex
) {
}
