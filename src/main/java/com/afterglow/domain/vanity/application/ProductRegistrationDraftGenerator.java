package com.afterglow.domain.vanity.application;

import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

/**
 * OCR raw text를 제품 등록 draft로 구조화하는 use-case 전용 contract — 범용 AI 호출 인터페이스가 아니다.
 * 구현체는 {@link ProductRegistrationDraft}에 없는 값(제품 원인 판정, 추천, 사용자 행동 정보 등)을
 * 만들어내면 안 되고, OCR text에 근거가 없는 필드는 null로 남겨야 한다.
 *
 * <p>실패(disabled/timeout/401/429/5xx/refusal/malformed 등)는 전부 {@link AfterglowException}
 * ({@link ErrorCode#AI_REQUEST_FAILED})으로 던진다. {@link com.afterglow.domain.episode.analysis.domain.AnalysisExplanationGenerator}와
 * 달리 이 use case에는 의미 있는 결정적 fallback이 없으므로, 호출자는 이 실패를 조용히 삼키지 않고
 * 그대로 API 실패로 전파해야 한다.
 */
public interface ProductRegistrationDraftGenerator {

	ProductRegistrationDraft generate(String rawText);
}
