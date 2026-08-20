package com.afterglow.domain.episode.infrastructure.ai.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OpenAiClient의 요청 조립/응답 파싱/오류 변환을 실제 OpenAI 호출 없이 검증한다(MockRestServiceServer).
 * 응답 mock은 실제 Responses API raw HTTP JSON 구조({@code output[].content[]}, top-level
 * {@code output_text} 없음)를 그대로 따른다 — SDK convenience property를 mock하지 않는다.
 */
class OpenAiClientTest {

	private static final String BASE_URL = "http://openai.test";

	private final RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
	private final OpenAiClient client =
			new OpenAiClient(restClientBuilder.build(), "gpt-test", new ObjectMapper());

	private final OpenAiJsonSchema fixtureSchema = new OpenAiJsonSchema(
			"sample_result",
			Map.of(
					"type", "object",
					"properties", Map.of("message", Map.of("type", "string")),
					"required", List.of("message"),
					"additionalProperties", false
			)
	);

	// ---------- 정상 경로 + 요청 조립 검증 ----------

	@Test
	void 요청을_올바르게_조립하고_completed_output_text를_역직렬화한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.model").value("gpt-test"))
				.andExpect(jsonPath("$.store").value(false))
				.andExpect(jsonPath("$.input[0].role").value("user"))
				.andExpect(jsonPath("$.input[0].content").value("hello"))
				.andExpect(jsonPath("$.text.format.type").value("json_schema"))
				.andExpect(jsonPath("$.text.format.name").value("sample_result"))
				.andExpect(jsonPath("$.text.format.strict").value(true))
				.andRespond(withSuccess(completedResponse("{\"message\":\"world\"}"), MediaType.APPLICATION_JSON));

		SampleResult result = client.createStructuredResponse("hello", fixtureSchema, SampleResult.class);

		assertThat(result.message()).isEqualTo("world");
		server.verify();
	}

	// ---------- status 검증 ----------

	@Test
	void incomplete_상태면_output_text가_있어도_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withSuccess(responseWithStatus("incomplete", "{\"message\":\"world\"}"), MediaType.APPLICATION_JSON));

		assertAiRequestFailed();
	}

	@Test
	void failed_상태면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withSuccess("""
						{"id": "resp_failed", "status": "failed"}
						""", MediaType.APPLICATION_JSON));

		assertAiRequestFailed();
	}

	@Test
	void cancelled_상태면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withSuccess("""
						{"id": "resp_cancelled", "status": "cancelled"}
						""", MediaType.APPLICATION_JSON));

		assertAiRequestFailed();
	}

	@Test
	void status가_없으면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withSuccess("""
						{"id": "resp_no_status"}
						""", MediaType.APPLICATION_JSON));

		assertAiRequestFailed();
	}

	// ---------- completed인데 정상 output이 없는 경우 ----------

	@Test
	void completed_이지만_refusal만_있으면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withSuccess("""
						{
						  "id": "resp_refusal",
						  "status": "completed",
						  "output": [
						    {"type": "message", "content": [{"type": "refusal", "refusal": "정책상 답변할 수 없습니다."}]}
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		assertAiRequestFailed();
	}

	@Test
	void completed_이지만_output이_빈_배열이면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withSuccess("""
						{"id": "resp_empty_output", "status": "completed", "output": []}
						""", MediaType.APPLICATION_JSON));

		assertAiRequestFailed();
	}

	@Test
	void completed_이지만_message_타입_output이_없으면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withSuccess("""
						{
						  "id": "resp_no_message",
						  "status": "completed",
						  "output": [
						    {"type": "reasoning", "content": []}
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		assertAiRequestFailed();
	}

	@Test
	void completed_이지만_output_text가_공백이면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withSuccess(completedResponse("   "), MediaType.APPLICATION_JSON));

		assertAiRequestFailed();
	}

	@Test
	void 여러_output_item_중_뒤쪽에_있는_output_text도_고정_인덱스_가정_없이_찾는다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withSuccess("""
						{
						  "id": "resp_multi",
						  "status": "completed",
						  "output": [
						    {"type": "reasoning", "content": []},
						    {
						      "type": "message",
						      "content": [
						        {"type": "refusal", "refusal": "일부 거부"},
						        {"type": "output_text", "text": "{\\"message\\":\\"second-item\\"}"}
						      ]
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		SampleResult result = client.createStructuredResponse("hello", fixtureSchema, SampleResult.class);

		assertThat(result.message()).isEqualTo("second-item");
	}

	// ---------- HTTP 오류 ----------

	@Test
	void 상태코드_401이면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertAiRequestFailed();
	}

	@Test
	void 상태코드_429면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertAiRequestFailed();
	}

	@Test
	void 상태코드_5xx_응답이면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withServerError());

		assertAiRequestFailed();
	}

	@Test
	void 연결_timeout류_오류도_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(request -> {
					throw new SocketTimeoutException("simulated timeout");
				});

		assertAiRequestFailed();
	}

	// ---------- 파싱 오류 ----------

	@Test
	void 구조화_응답의_output_text가_유효한_JSON이_아니면_AI_REQUEST_FAILED로_변환한다() {
		server.expect(requestTo(BASE_URL + "/responses"))
				.andRespond(withSuccess(completedResponse("이건 JSON이 아닙니다"), MediaType.APPLICATION_JSON));

		assertAiRequestFailed();
	}

	private void assertAiRequestFailed() {
		assertThatThrownBy(() -> client.createStructuredResponse("hello", fixtureSchema, SampleResult.class))
				.isInstanceOf(AfterglowException.class)
				.extracting(e -> ((AfterglowException) e).getErrorCode())
				.isEqualTo(ErrorCode.AI_REQUEST_FAILED);
	}

	/** 실제 Responses API raw 응답 구조 그대로: status=completed, output[].content[](type=output_text).text. */
	private String completedResponse(String outputText) {
		return responseWithStatus("completed", outputText);
	}

	private String responseWithStatus(String status, String outputText) {
		String escaped = outputText.replace("\\", "\\\\").replace("\"", "\\\"");
		return """
				{
				  "id": "resp_test",
				  "status": "%s",
				  "output": [
				    {
				      "type": "message",
				      "content": [
				        {"type": "output_text", "text": "%s"}
				      ]
				    }
				  ]
				}
				""".formatted(status, escaped);
	}

	/** 테스트 전용 fixture — 실제 Analysis 응답 schema가 아니다. */
	private record SampleResult(String message) {
	}
}
