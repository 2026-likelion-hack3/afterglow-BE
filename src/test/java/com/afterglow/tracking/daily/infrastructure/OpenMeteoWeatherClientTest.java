package com.afterglow.tracking.daily.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class OpenMeteoWeatherClientTest {

	@Test
	void 서울의_현재_날씨를_가져온다() {
		OpenMeteoWeatherClient client =
			new OpenMeteoWeatherClient(new ObjectMapper());

		WeatherData weather = client.getCurrentWeather(
			37.5665,
			126.9780
		);

		assertThat(weather).isNotNull();
		assertThat(weather.temperature()).isNotNull();
		assertThat(weather.humidity()).isNotNull();
		assertThat(weather.uvIndex()).isNotNull();
	}
}
