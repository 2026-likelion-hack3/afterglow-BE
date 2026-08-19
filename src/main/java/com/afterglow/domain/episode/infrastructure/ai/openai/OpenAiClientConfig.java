package com.afterglow.domain.episode.infrastructure.ai.openai;

import java.net.http.HttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * afterglow.openai.enabled=false(기본값)면 {@link OpenAiClient} 빈 자체를 만들지 않는다 — 아직 실제
 * 소비자가 없으므로 API key/model 미설정 상태로도 애플리케이션 전체가 정상 기동해야 한다. enabled=true일
 * 때만 필수값을 fail-fast로 검증한다.
 */
@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiClientConfig {

	@Bean
	@ConditionalOnProperty(prefix = "afterglow.openai", name = "enabled", havingValue = "true")
	OpenAiClient openAiClient(OpenAiProperties properties, ObjectMapper objectMapper) {
		validate(properties);

		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.connectTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());

		RestClient restClient = RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
				.build();

		return new OpenAiClient(restClient, properties.model(), objectMapper);
	}

	private void validate(OpenAiProperties properties) {
		if (!StringUtils.hasText(properties.apiKey())) {
			throw new IllegalStateException("afterglow.openai.enabled=true인데 OPENAI_API_KEY가 설정되지 않았습니다.");
		}
		if (!StringUtils.hasText(properties.model())) {
			throw new IllegalStateException("afterglow.openai.enabled=true인데 OPENAI_MODEL이 설정되지 않았습니다.");
		}
	}
}
