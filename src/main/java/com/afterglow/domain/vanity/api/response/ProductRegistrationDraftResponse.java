package com.afterglow.domain.vanity.api.response;

import java.util.Set;

import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.application.ProductRegistrationDraftResult;

/**
 * 필드명은 {@link com.afterglow.domain.vanity.api.request.ProductCreateRequest}와 그대로 맞춘다 — 프론트가
 * 이 값을 그대로 등록 폼에 채워 넣을 수 있게. {@code interactionTags}는 AI가 만든 값이 아니라 서버가
 * {@code keyIngredients}에서 결정적으로 매칭한 값이다({@code HIGH_CONCENTRATION}/{@code LOW_IRRITATION}은
 * 이번 범위에서 매칭하지 않아 항상 비어 있을 수 있다) — 등록 화면에서 사용자가 그대로 확인/수정한 뒤
 * 기존 {@code POST /api/vanity/products}로 넘기는 초안일 뿐, 이 응답 자체가 무언가를 저장하지 않는다.
 */
public record ProductRegistrationDraftResponse(
		String name,
		String brand,
		String type,
		String keyIngredients,
		Set<InteractionTag> interactionTags
) {

	public static ProductRegistrationDraftResponse from(ProductRegistrationDraftResult result) {
		return new ProductRegistrationDraftResponse(
				result.draft().name(),
				result.draft().brand(),
				result.draft().type(),
				result.draft().keyIngredients(),
				result.interactionTags()
		);
	}
}
