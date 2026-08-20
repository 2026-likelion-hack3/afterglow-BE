package com.afterglow.domain.episode.analysis.domain;

import java.util.List;

/**
 * 이미 확정된 {@link com.afterglow.domain.episode.card.domain.ResultCardResult}를 사람이 읽기 쉬운
 * 문장으로 옮긴 것 — AI({@code OpenAiAnalysisExplanationGenerator})와 결정적 fallback
 * ({@link AnalysisExplanationFallback}) 양쪽이 이 형태로 만든다. 이 타입 자체는 아무 판단도 하지
 * 않는다 — 이미 저장된 hold/holdReason/confidence/cards를 문장으로 바꾼 결과물일 뿐이다.
 *
 * @param headline      한 줄 제목
 * @param summary       핵심 요약 문장
 * @param evidenceNotes 근거를 풀어 쓴 문장 목록 — 없으면 빈 리스트(hold일 때 등)
 * @param nextAction    다음에 할 일 한 줄
 */
public record AnalysisExplanation(
		String headline,
		String summary,
		List<String> evidenceNotes,
		String nextAction
) {
	public AnalysisExplanation {
		evidenceNotes = evidenceNotes == null ? List.of() : List.copyOf(evidenceNotes);
	}
}
