package com.afterglow.domain.episode.infrastructure.ai.openai;

import java.util.Map;

/**
 * Responses API structured output({@code text.format})에 넘길 JSON Schema.
 * 각 use case가 자신의 schema를 들고 이 타입을 채워 호출한다 — 다른 패키지의 소비자를 위해 최소한으로
 * public을 열었을 뿐, schema 내용 자체는 여전히 각 use case 전용이다.
 */
public record OpenAiJsonSchema(String name, Map<String, Object> schema) {
}
