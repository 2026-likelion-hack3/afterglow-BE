package com.afterglow.domain.vanity.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.afterglow.domain.episode.infrastructure.ai.openai.OpenAiClient;
import com.afterglow.domain.vanity.application.ProductRegistrationDraftGenerator;

/**
 * {@code afterglow.openai.enabled=false}(기본값)면 {@link ProductRegistrationDraftGenerator} 빈
 * 자체를 만들지 않는다 — {@code OpenAiClientConfig}가 이미 같은 조건으로 {@link OpenAiClient} 빈을
 * 만들지 않으므로, credential 없이도 애플리케이션 전체가 정상 기동해야 한다. 새 RestClient/OpenAI
 * HTTP client를 만들지 않고 기존 {@link OpenAiClient} 빈을 그대로 재사용한다.
 *
 * <p>소비하는 쪽({@code ProductRegistrationDraftService})은 {@code Optional<ProductRegistrationDraftGenerator>}로
 * 주입받아, 빈이 없을 때 명시적으로 실패 처리한다({@code AnalysisExplanationGenerator}와 달리 결정적
 * fallback이 없다).
 */
@Configuration
public class VanityAiConfig {

	@Bean
	@ConditionalOnProperty(prefix = "afterglow.openai", name = "enabled", havingValue = "true")
	ProductRegistrationDraftGenerator productRegistrationDraftGenerator(OpenAiClient openAiClient) {
		return new OpenAiProductRegistrationDraftGenerator(openAiClient);
	}
}
