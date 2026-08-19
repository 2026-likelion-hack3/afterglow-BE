package com.afterglow.domain.episode.infrastructure.ai.openai;

import java.util.Map;

/**
 * Responses API structured output({@code text.format})에 넘길 JSON Schema.
 * 실제 비즈니스 스키마는 이번 범위에 없다 — 첫 실제 use case가 자신의 schema를 들고 이 타입을 채워 호출한다.
 */
record OpenAiJsonSchema(String name, Map<String, Object> schema) {
}
