package com.afterglow.domain.vanity.application;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * OCR raw text를 {@link ProductRegistrationDraft}로 구조화한다 — 아무것도 저장하지 않는다(읽기 전용).
 * {@link ProductRegistrationDraftGenerator} 빈이 없으면(afterglow.openai.enabled=false) 그 자체를
 * {@link ErrorCode#AI_REQUEST_FAILED}로 취급한다 — {@code EpisodeAnalysisExplanationService}와 달리
 * 결정적 fallback으로 조용히 넘어가지 않고 실패를 그대로 호출자에게 알린다.
 *
 * <p>AI가 만든 draft의 {@code keyIngredients}에 {@link InteractionTagMatcher}(결정적 성분명 매칭,
 * AI 아님)를 적용해 {@link InteractionTag} 후보를 함께 돌려준다 — 매칭 근거가 없으면 빈 Set이다.
 */
@Service
@RequiredArgsConstructor
public class ProductRegistrationDraftService {

	private final Optional<ProductRegistrationDraftGenerator> productRegistrationDraftGenerator;

	public ProductRegistrationDraftResult createDraft(String rawText) {
		if (!StringUtils.hasText(rawText)) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "OCR 텍스트가 필요합니다.");
		}

		ProductRegistrationDraft draft = productRegistrationDraftGenerator
				.orElseThrow(() -> new AfterglowException(ErrorCode.AI_REQUEST_FAILED))
				.generate(rawText);

		Set<InteractionTag> interactionTags = InteractionTagMatcher.match(draft.keyIngredients());

		return new ProductRegistrationDraftResult(draft, interactionTags);
	}
}
