package com.afterglow.tracking.daily.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OpenMeteoWeatherClient implements WeatherClient {

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public OpenMeteoWeatherClient(ObjectMapper objectMapper) {
		this.httpClient = HttpClient.newHttpClient();
		this.objectMapper = objectMapper;
	}

	@Override
	public WeatherData getCurrentWeather(double latitude, double longitude) {
		try {
			String url = String.format(
				"https://api.open-meteo.com/v1/forecast"
					+ "?latitude=%s"
					+ "&longitude=%s"
					+ "&current=temperature_2m,relative_humidity_2m,uv_index",
				latitude,
				longitude
			);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
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

			JsonNode current = objectMapper
				.readTree(response.body())
				.path("current");

			return new WeatherData(
				current.path("temperature_2m").asDouble(),
				current.path("relative_humidity_2m").asDouble(),
				current.path("uv_index").asDouble()
			);

		} catch (Exception e) {
			throw new IllegalStateException("날씨 정보를 가져오지 못했습니다.", e);
		}
	}
}
