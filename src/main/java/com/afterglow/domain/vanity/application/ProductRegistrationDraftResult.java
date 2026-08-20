package com.afterglow.domain.vanity.application;

import java.util.Set;

import com.afterglow.domain.vanity.InteractionTag;

/**
 * {@link ProductRegistrationDraftService#createDraft}의 반환값 — AI가 만든 {@link ProductRegistrationDraft}와
 * 서버가 {@code keyIngredients}에서 결정적으로 매칭한 {@link InteractionTag}를 함께 담는다.
 * {@code interactionTags}는 {@link InteractionTagMatcher}로만 채워진다 — AI structured output에는
 * 이 필드가 없다(스키마에 아예 없음).
 */
public record ProductRegistrationDraftResult(
		ProductRegistrationDraft draft,
		Set<InteractionTag> interactionTags
) {
}
