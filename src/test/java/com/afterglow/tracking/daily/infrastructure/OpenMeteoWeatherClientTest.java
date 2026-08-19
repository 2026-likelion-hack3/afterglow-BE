package com.afterglow.tracking.daily.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class OpenMeteoWeatherClientTest {

	@Test
	void 서울의_날짜별_날씨를_가져온다() {
		OpenMeteoWeatherClient client =
			new OpenMeteoWeatherClient(new ObjectMapper());

		WeatherData weather = client.getWeatherByDate(
			37.5665,
			126.9780,
			LocalDate.of(2026, 8, 16)
		);

		assertThat(weather).isNotNull();
		assertThat(weather.temperature()).isNotNull();
		assertThat(weather.humidity()).isNotNull();
		assertThat(weather.uvIndex()).isNotNull();
	}
}
