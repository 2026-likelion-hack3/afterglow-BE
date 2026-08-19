package com.afterglow.domain.episode.infrastructure.ai.openai;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OpenAI Responses API({@code POST /responses}) 호출 어댑터.
 * 아직 실제 소비자가 없어 패키지 밖으로 노출하지 않는다 — 첫 실제 use case가 생기면 그 use case 전용
 * application 계층 port가 이 클래스를 감싸는 방향으로 간다(이 클래스 자체를 범용 API로 넓히지 않는다).
 */
class OpenAiClient {

	private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
	private static final String REQUEST_ID_HEADER = "x-request-id";
	private static final String STATUS_COMPLETED = "completed";
	private static final String TYPE_MESSAGE = "message";
	private static final String TYPE_OUTPUT_TEXT = "output_text";

	private final RestClient restClient;
	private final String model;
	private final ObjectMapper objectMapper;

	OpenAiClient(RestClient restClient, String model, ObjectMapper objectMapper) {
		this.restClient = restClient;
		this.model = model;
		this.objectMapper = objectMapper;
	}

	/** {@code input}을 user 메시지로 보내고, {@code schema}로 구조화된 응답을 {@code responseType}으로 역직렬화해 돌려준다. */
	<T> T createStructuredResponse(String input, OpenAiJsonSchema schema, Class<T> responseType) {
		OpenAiResponseRequest request = new OpenAiResponseRequest(
				model,
				List.of(new OpenAiInputMessage("user", input)),
				new OpenAiTextConfig(new OpenAiTextFormat("json_schema", schema.name(), schema.schema(), true)),
				false
		);

		OpenAiResponseEnvelope response = call(request);
		String outputText = extractOutputText(response);
		return parse(outputText, responseType, response.id());
	}

	private OpenAiResponseEnvelope call(OpenAiResponseRequest request) {
		try {
			return restClient.post()
					.uri("/responses")
					.body(request)
					.retrieve()
					.body(OpenAiResponseEnvelope.class);
		} catch (RestClientResponseException e) {
			log.error("OpenAI 응답 실패 status={} requestId={}",
					e.getStatusCode().value(), e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst(REQUEST_ID_HEADER) : null);
			throw new AfterglowException(ErrorCode.AI_REQUEST_FAILED);
		} catch (ResourceAccessException e) {
			log.error("OpenAI 호출 실패(연결/timeout) model={}", model, e);
			throw new AfterglowException(ErrorCode.AI_REQUEST_FAILED);
		}
	}

	/**
	 * raw HTTP 응답에는 SDK의 {@code response.output_text} 같은 top-level 편의 필드가 없다 — 반드시
	 * {@code output[].content[]}를 직접 훑어서 {@code type == "output_text"}인 항목의 {@code text}를 찾는다.
	 * {@code output[0]}/{@code content[0]} 같은 고정 인덱스 가정은 두지 않는다.
	 */
	private String extractOutputText(OpenAiResponseEnvelope response) {
		if (response == null) {
			log.error("OpenAI 응답 본문이 비어 있음");
			throw new AfterglowException(ErrorCode.AI_REQUEST_FAILED);
		}
		if (!STATUS_COMPLETED.equals(response.status())) {
			log.error("OpenAI 응답이 완료 상태가 아님 responseId={} status={}", response.id(), response.status());
			throw new AfterglowException(ErrorCode.AI_REQUEST_FAILED);
		}
		if (response.output() == null) {
			log.error("OpenAI 완료 응답에 output이 없음 responseId={}", response.id());
			throw new AfterglowException(ErrorCode.AI_REQUEST_FAILED);
		}

		return response.output().stream()
				.filter(item -> TYPE_MESSAGE.equals(item.type()))
				.flatMap(item -> item.content() == null ? Stream.empty() : item.content().stream())
				.filter(content -> TYPE_OUTPUT_TEXT.equals(content.type()))
				.map(OpenAiContentItem::text)
				.filter(StringUtils::hasText)
				.findFirst()
				.orElseGet(() -> {
					// message가 없거나, content가 output_text가 아니거나(refusal 등), text가 비어 있는 경우 전부 포함.
					log.error("OpenAI 완료 응답에서 유효한 output_text를 찾지 못함 responseId={}", response.id());
					throw new AfterglowException(ErrorCode.AI_REQUEST_FAILED);
				});
	}

	private <T> T parse(String json, Class<T> responseType, String responseId) {
		try {
			return objectMapper.readValue(json, responseType);
		} catch (JsonProcessingException e) {
			log.error("OpenAI 구조화 응답 파싱 실패 responseId={}", responseId);
			throw new AfterglowException(ErrorCode.AI_REQUEST_FAILED);
		}
	}

	private record OpenAiResponseRequest(String model, List<OpenAiInputMessage> input, OpenAiTextConfig text, boolean store) {
	}

	private record OpenAiInputMessage(String role, String content) {
	}

	private record OpenAiTextConfig(OpenAiTextFormat format) {
	}

	private record OpenAiTextFormat(String type, String name, Map<String, Object> schema, boolean strict) {
	}

	private record OpenAiResponseEnvelope(String id, String status, List<OpenAiOutputItem> output) {
	}

	private record OpenAiOutputItem(String type, List<OpenAiContentItem> content) {
	}

	private record OpenAiContentItem(String type, String text) {
	}
}
