package com.afterglow.domain.episode.analysis.domain;

import com.afterglow.domain.episode.card.domain.ResultCardResult;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

/**
 * 이미 확정된 {@link ResultCardResult}를 사람이 읽기 쉬운 문장으로 설명하는 use-case 전용 contract —
 * 범용 AI 호출 인터페이스가 아니다. 구현체는 원인 후보를 새로 만들거나 카드 순서/근거/확신 단계/HOLD
 * 여부를 바꾸면 안 된다 — 오직 주어진 값을 문장으로 옮기기만 한다.
 *
 * <p>실패(timeout/401/429/5xx/refusal/malformed 등)는 전부 {@link AfterglowException}
 * ({@link ErrorCode#AI_REQUEST_FAILED})으로 던진다 — 이 실패를 잡아 결정적 fallback으로 넘어갈지는
 * 호출자(application 계층)의 책임이다.
 */
public interface AnalysisExplanationGenerator {

	AnalysisExplanation generate(ResultCardResult analysisResult);
}
