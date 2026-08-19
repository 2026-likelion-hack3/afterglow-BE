package com.afterglow.domain.episode.infrastructure.ai.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

/**
 * OpenAiClientConfig가 실제로 조립하는 RestClient(Authorization 헤더, Content-Type, base-url,
 * connect/read timeout)가 진짜로 반영되는지 로컬 stub HTTP 서버(JDK 내장, 새 의존성 없음)로 검증한다.
 * 실제 OpenAI 서버는 호출하지 않는다.
 */
class OpenAiClientConfigWiringTest {

	private static final String COMPLETED_RESPONSE_JSON = """
			{
			  "id": "resp_wiring",
			  "status": "completed",
			  "output": [
			    {"type": "message", "content": [{"type": "output_text", "text": "{\\"message\\":\\"world\\"}"}]}
			  ]
			}
			""";

	private static final OpenAiJsonSchema FIXTURE_SCHEMA = new OpenAiJsonSchema(
			"sample_result",
			Map.of("type", "object", "properties", Map.of("message", Map.of("type", "string")))
	);

	private HttpServer stubServer;

	@AfterEach
	void tearDown() {
		if (stubServer != null) {
			stubServer.stop(0);
		}
	}

	@Test
	void enabled_true면_Authorization_ContentType_baseUrl이_실제_요청에_반영된다() throws IOException {
		Headers[] capturedHeaders = new Headers[1];
		String[] capturedBody = new String[1];

		stubServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		stubServer.createContext("/responses", exchange -> {
			capturedHeaders[0] = exchange.getRequestHeaders();
			capturedBody[0] = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

			byte[] body = COMPLETED_RESPONSE_JSON.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		stubServer.start();
		int port = stubServer.getAddress().getPort();

		new ApplicationContextRunner()
				.withBean(ObjectMapper.class, ObjectMapper::new)
				.withUserConfiguration(OpenAiClientConfig.class)
				.withPropertyValues(
						"afterglow.openai.enabled=true",
						"afterglow.openai.api-key=sk-wiring-test",
						"afterglow.openai.model=gpt-test",
						"afterglow.openai.base-url=http://localhost:" + port,
						"afterglow.openai.connect-timeout=2s",
						"afterglow.openai.read-timeout=2s")
				.run(context -> {
					OpenAiClient client = context.getBean(OpenAiClient.class);
					SampleResult result = client.createStructuredResponse("hello", FIXTURE_SCHEMA, SampleResult.class);
					assertThat(result.message()).isEqualTo("world");
				});

		assertThat(capturedHeaders[0].getFirst("Authorization")).isEqualTo("Bearer sk-wiring-test");
		assertThat(capturedHeaders[0].getFirst("Content-Type")).startsWith("application/json");
		assertThat(capturedBody[0]).contains("\"model\":\"gpt-test\"");
	}

	@Test
	void read_timeout보다_응답이_늦으면_AI_REQUEST_FAILED로_실패한다() throws IOException {
		stubServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		stubServer.createContext("/responses", exchange -> {
			try {
				Thread.sleep(600);
				byte[] body = COMPLETED_RESPONSE_JSON.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
				exchange.close();
			} catch (Exception e) {
				exchange.close();
			}
		});
		stubServer.start();
		int port = stubServer.getAddress().getPort();

		new ApplicationContextRunner()
				.withBean(ObjectMapper.class, ObjectMapper::new)
				.withUserConfiguration(OpenAiClientConfig.class)
				.withPropertyValues(
						"afterglow.openai.enabled=true",
						"afterglow.openai.api-key=sk-wiring-test",
						"afterglow.openai.model=gpt-test",
						"afterglow.openai.base-url=http://localhost:" + port,
						"afterglow.openai.connect-timeout=2s",
						"afterglow.openai.read-timeout=200ms")
				.run(context -> {
					OpenAiClient client = context.getBean(OpenAiClient.class);
					assertThatThrownBy(() -> client.createStructuredResponse("hello", FIXTURE_SCHEMA, SampleResult.class))
							.isInstanceOf(AfterglowException.class)
							.extracting(e -> ((AfterglowException) e).getErrorCode())
							.isEqualTo(ErrorCode.AI_REQUEST_FAILED);
				});
	}

	private record SampleResult(String message) {
	}
}
