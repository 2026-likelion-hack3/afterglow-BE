package com.afterglow.domain.episode.infrastructure.ai.openai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

/** afterglow.openai.enabled 값에 따른 OpenAiClient 빈 생성 여부 + fail-fast 검증. 실제 OpenAI 호출 없음. */
class OpenAiClientConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withBean(ObjectMapper.class, ObjectMapper::new)
			.withUserConfiguration(OpenAiClientConfig.class);

	@Test
	void enabled가_false면_api_key와_model이_없어도_정상_기동하고_빈이_생성되지_않는다() {
		runner.withPropertyValues("afterglow.openai.enabled=false")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).doesNotHaveBean(OpenAiClient.class);
				});
	}

	@Test
	void enabled_설정_자체가_없어도_기본값false로_정상_기동하고_빈이_생성되지_않는다() {
		runner.run((AssertableApplicationContext context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(OpenAiClient.class);
		});
	}

	@Test
	void enabled가_true인데_api_key가_없으면_기동에_실패한다() {
		runner.withPropertyValues(
						"afterglow.openai.enabled=true",
						"afterglow.openai.api-key=",
						"afterglow.openai.model=gpt-test",
						"afterglow.openai.base-url=http://openai.test",
						"afterglow.openai.connect-timeout=1s",
						"afterglow.openai.read-timeout=1s")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void enabled가_true인데_model이_없으면_기동에_실패한다() {
		runner.withPropertyValues(
						"afterglow.openai.enabled=true",
						"afterglow.openai.api-key=sk-test",
						"afterglow.openai.model=",
						"afterglow.openai.base-url=http://openai.test",
						"afterglow.openai.connect-timeout=1s",
						"afterglow.openai.read-timeout=1s")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void enabled가_true이고_api_key_model이_모두_있으면_빈이_생성된다() {
		runner.withPropertyValues(
						"afterglow.openai.enabled=true",
						"afterglow.openai.api-key=sk-test",
						"afterglow.openai.model=gpt-test",
						"afterglow.openai.base-url=http://openai.test",
						"afterglow.openai.connect-timeout=1s",
						"afterglow.openai.read-timeout=1s")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(OpenAiClient.class);
				});
	}

	@Test
	void connect_read_timeout이_Duration으로_정확히_바인딩된다() {
		runner.withPropertyValues(
						"afterglow.openai.enabled=false",
						"afterglow.openai.connect-timeout=3s",
						"afterglow.openai.read-timeout=45s")
				.run(context -> {
					OpenAiProperties properties = context.getBean(OpenAiProperties.class);
					assertThat(properties.connectTimeout()).isEqualTo(java.time.Duration.ofSeconds(3));
					assertThat(properties.readTimeout()).isEqualTo(java.time.Duration.ofSeconds(45));
				});
	}
}
