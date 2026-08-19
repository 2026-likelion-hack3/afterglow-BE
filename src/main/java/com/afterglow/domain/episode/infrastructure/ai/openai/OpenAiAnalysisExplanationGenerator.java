package com.afterglow.domain.episode.infrastructure.ai.openai;

import java.util.List;
import java.util.Map;

import com.afterglow.domain.episode.analysis.domain.AnalysisExplanation;
import com.afterglow.domain.episode.analysis.domain.AnalysisExplanationGenerator;
import com.afterglow.domain.episode.card.domain.ResultCardResult;

/**
 * {@link AnalysisExplanationGenerator}의 OpenAI Responses API 구현체 — "이미 확정된 Analysis 결과를
 * 문장으로 설명하는" 첫 실제 {@link OpenAiClient} 소비자다. 판단은 전혀 하지 않는다: {@code
 * SYSTEM_INSTRUCTION}이 원인/카드/근거/확신/HOLD를 바꾸지 말라고 명시하고, 실제 판단 데이터는
 * {@link ResultCardResult}를 그대로 프롬프트에 실어 보낸다.
 *
 * <p>기존 {@link OpenAiClient#createStructuredResponse}는 system/user 역할을 구분하지 않고 단일
 * 텍스트만 받는다 — 이 어댑터를 위해 그 계약을 넓히지 않고, system 지시문과 데이터를 하나의 입력
 * 텍스트로 합쳐서 보낸다(기존 OpenAI infra 구조를 바꾸지 않기 위한 의도적 선택).
 */
class OpenAiAnalysisExplanationGenerator implements AnalysisExplanationGenerator {

	private static final String SYSTEM_INSTRUCTION = """
			당신은 피부 자극 원인 분석 결과를 사용자에게 쉬운 문장으로 설명하는 어시스턴트다.
			아래 규칙을 반드시 지켜라.

			1. "분석 결과"에 주어진 hold, holdReason, confidence, cards 값은 이미 규칙 기반 엔진이
			   확정한 것이다. 이 값을 그대로 신뢰하고 절대 바꾸지 마라.
			2. 새로운 원인을 만들어내지 마라. cards의 순서, causeType, evidence, confidence, hold
			   여부를 바꾸거나 반박하지 마라.
			3. hold가 true이면 원인을 확정할 수 없는 상태다 — headline/summary에서 원인이 확정된
			   것처럼 쓰지 마라. "~때문일 수 있다" 같은 표현도 쓰지 말고, 아직 판단하기 이르다는
			   사실을 분명히 남겨라.
			4. 존재하지 않는 제품·성분·브랜드를 만들어내거나 언급하지 마라.
			5. 신규 제품 구매를 권하거나 특정 제품을 추천하지 마라.
			6. 루틴 구성이나 어떤 제품을 써야 할지 새로 결정하지 마라 — 이미 있는 카드 내용만 설명하라.
			7. 3일차 판정(유지/연장/중단)을 대신 내리지 마라.
			8. 의료 진단을 내리거나 치료법을 제시하지 마라.
			9. 병원 방문 기준을 새로 만들거나 바꾸지 마라.
			10. 반드시 headline, summary, evidenceNotes, nextAction 네 필드만 있는 JSON으로 답하라.

			아래는 이미 확정된 분석 결과다. 위 규칙을 지키며 사용자에게 설명하는 문장으로 바꿔라.

			""";

	private static final OpenAiJsonSchema SCHEMA = new OpenAiJsonSchema(
			"analysis_explanation",
			Map.of(
					"type", "object",
					"properties", Map.of(
							"headline", Map.of("type", "string"),
							"summary", Map.of("type", "string"),
							"evidenceNotes", Map.of("type", "array", "items", Map.of("type", "string")),
							"nextAction", Map.of("type", "string")
					),
					"required", List.of("headline", "summary", "evidenceNotes", "nextAction"),
					"additionalProperties", false
			)
	);

	private final OpenAiClient openAiClient;

	OpenAiAnalysisExplanationGenerator(OpenAiClient openAiClient) {
		this.openAiClient = openAiClient;
	}

	@Override
	public AnalysisExplanation generate(ResultCardResult analysisResult) {
		String prompt = SYSTEM_INSTRUCTION + describe(analysisResult);
		return openAiClient.createStructuredResponse(prompt, SCHEMA, AnalysisExplanation.class);
	}

	private String describe(ResultCardResult analysisResult) {
		return "분석 결과: hold=" + analysisResult.hold()
				+ ", holdReason=" + analysisResult.holdReason()
				+ ", confidence=" + analysisResult.confidence()
				+ ", cards=" + analysisResult.cards();
	}
}
