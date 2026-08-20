package com.afterglow.domain.episode.analysis.domain;

import java.util.List;

import com.afterglow.domain.episode.card.domain.ResultCard;
import com.afterglow.domain.episode.card.domain.ResultCardResult;

/**
 * OpenAI가 비활성/미설정이거나 호출에 실패했을 때 쓰는 결정적(pure) 설명 생성기 — 새로운 분석 규칙을
 * 만들지 않고, 이미 저장된 {@link ResultCardResult}의 값만으로 문장을 조립한다.
 * {@link com.afterglow.domain.episode.checkin.domain.Day3JudgmentEngine}과 동일한 스타일로 Repository/
 * Spring 의존이 없다.
 *
 * <p>hold일 때는 원인이 확정되지 않았다는 사실을 절대 숨기지 않는다(headline/summary 모두). hold가
 * 아닐 때는 "~때문이다" 같은 단정적 표현 대신 "가능성이 높아 보인다" 수준으로만 서술하고, confidence가
 * NORMAL이면 기존 카드 문구 규칙(docs/domains/episode.md Confirmed Decisions)과 동일하게 "기록이 더
 * 모이면 확실해진다"는 안내를 덧붙인다.
 */
public final class AnalysisExplanationFallback {

	public AnalysisExplanation explain(ResultCardResult analysisResult) {
		if (analysisResult.hold()) {
			return explainHold(analysisResult);
		}
		return explainDetermined(analysisResult);
	}

	private AnalysisExplanation explainHold(ResultCardResult analysisResult) {
		String headline = "아직 원인을 확정하기 어려워요";
		String summary = switch (analysisResult.holdReason()) {
			case INSUFFICIENT_RECORDS, INCONCLUSIVE_EVIDENCE ->
					"지금까지 기록만으로는 원인을 하나로 좁히기 어려워요. 기록이 더 쌓이면 다시 알려드릴게요.";
			case NO_TARGET -> "지금 조건에서는 살펴볼 원인 후보 자체가 없어요.";
		};
		return new AnalysisExplanation(headline, summary, List.of(), "며칠 더 기록을 남겨주세요.");
	}

	private AnalysisExplanation explainDetermined(ResultCardResult analysisResult) {
		ResultCard topCard = analysisResult.cards().get(0);
		String confidenceNote = analysisResult.confidence() == Confidence.NORMAL
				? " 기록이 더 모이면 더 확실해질 수 있어요."
				: "";
		String summary = "가장 가능성이 높아 보이는 원인을 확인했어요." + confidenceNote;

		return new AnalysisExplanation(headlineFor(topCard), summary, evidenceNotesFor(topCard), nextActionFor(topCard));
	}

	private String headlineFor(ResultCard card) {
		return switch (card.type()) {
			case DISCONTINUE -> "최근 사용을 시작한 항목이 관련 있어 보여요.";
			case DO_MORE_TODAY -> "요즘 날씨가 영향을 줬을 수 있어요.";
			default -> "분석 결과를 확인했어요.";
		};
	}

	private List<String> evidenceNotesFor(ResultCard card) {
		return switch (card.evidence()) {
			case TimingEvidence timing -> List.of(
					"사용 시작일(" + timing.usageStartDate() + ")이 증상 시작일(" + timing.symptomStartDate() + ") 근처예요.");
			case CombinationEvidence combination -> List.of(
					combination.tagA() + "와(과) " + combination.tagB() + " 조합을 근거로 봤어요.");
			case FrequencyEvidence frequency -> List.of(
					"전체 " + frequency.observationCount() + "번 중 " + frequency.matchedObservationCount() + "번 증상과 겹쳤어요.");
			case null -> List.of();
		};
	}

	private String nextActionFor(ResultCard card) {
		return switch (card.type()) {
			case DISCONTINUE -> "카드에서 안내한 항목의 사용을 잠시 쉬어보는 걸 고려해보세요.";
			case DO_MORE_TODAY -> "카드에서 제안한 케어를 조금 더 챙겨보세요.";
			default -> "카드 내용을 확인해보세요.";
		};
	}
}
