package com.afterglow.domain.vanity.application;

/**
 * OCR raw text에서 자동으로 채울 수 있는 {@link com.afterglow.domain.vanity.api.request.ProductCreateRequest}
 * 필드만 담는다 — 필드명은 그 request/{@link com.afterglow.domain.vanity.Product}와 그대로 맞춘다.
 *
 * <p>{@code openedAt}/{@code openingPeriod}/{@code usageTiming}/{@code usageTimingChangedAt}/
 * {@code interactionTags}/{@code functionTags}/{@code barcode}/{@code photoKey}/
 * {@code registrationSource}는 의도적으로 이 타입에 없다 — 사용자 행동·상태 정보이거나(OpeningPeriod,
 * UsageTiming 등), 라벨 텍스트를 있는 그대로 옮기는 것을 넘어서는 분류 판단이 필요해 taxonomy가
 * 확정되지 않았다(functionTags/interactionTags — Pending Decision, docs/domains 참고). AI가 이 필드들을
 * 만들어내지 못하도록 타입 수준에서 막는다.
 *
 * @param name           라벨에서 읽은 제품명, 근거 없으면 null
 * @param brand          라벨에서 읽은 브랜드명, 근거 없으면 null
 * @param type           라벨에서 읽은 제품 유형(자유 형식, 예: 토너/크림), 근거 없으면 null
 * @param keyIngredients 라벨 성분표에서 읽은 성분 목록 원문, 근거 없으면 null
 */
public record ProductRegistrationDraft(
		String name,
		String brand,
		String type,
		String keyIngredients
) {
}
