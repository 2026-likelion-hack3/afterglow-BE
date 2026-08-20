package com.afterglow.domain.episode.infrastructure.ai.openai;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** enabled=false면 apiKey/model이 비어 있어도 된다 — {@link OpenAiClientConfig}가 enabled일 때만 검증한다. */
@ConfigurationProperties(prefix = "afterglow.openai")
public record OpenAiProperties(
		boolean enabled,
		String apiKey,
		String model,
		String baseUrl,
		Duration connectTimeout,
		Duration readTimeout
) {
}
