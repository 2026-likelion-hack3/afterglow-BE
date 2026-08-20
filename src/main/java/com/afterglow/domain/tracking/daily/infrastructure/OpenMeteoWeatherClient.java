package com.afterglow.domain.tracking.daily.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OpenMeteoWeatherClient implements WeatherClient {

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public OpenMeteoWeatherClient(ObjectMapper objectMapper) {
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();
		this.objectMapper = objectMapper;
	}

	@Override
	public WeatherData getWeatherByDate(
		double latitude,
		double longitude,
		LocalDate date
	) {
		try {
			String url = String.format(
				"https://historical-forecast-api.open-meteo.com/v1/forecast"
					+ "?latitude=%s"
					+ "&longitude=%s"
					+ "&start_date=%s"
					+ "&end_date=%s"
					+ "&daily=temperature_2m_mean,temperature_2m_min,relative_humidity_2m_mean,uv_index_max"
					+ "&timezone=auto",
				latitude,
				longitude,
				date,
				date
			);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofSeconds(10))
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofString()
			);

			if (response.statusCode() != 200) {
				throw new IllegalStateException(
					"날씨 API 호출에 실패했습니다. status=" + response.statusCode()
				);
			}

			JsonNode daily = objectMapper
				.readTree(response.body())
				.path("daily");

			return new WeatherData(
				daily.path("temperature_2m_mean").get(0).asDouble(),
				daily.path("temperature_2m_min").get(0).asDouble(),
				daily.path("relative_humidity_2m_mean").get(0).asDouble(),
				daily.path("uv_index_max").get(0).asDouble()
			);

		} catch (Exception e) {
			throw new IllegalStateException(
				"날짜별 날씨 정보를 가져오지 못했습니다.",
				e
			);
		}
	}
}
