package com.afterglow.domain.episode.analysis.domain;

/**
 * "제품 조합" 후보 하나(충돌 태그를 가진 제품 쌍)를 엔진이 판단할 수 있을 만큼 정규화한 입력.
 * Vanity의 {@code InteractionTag} enum을 직접 참조하지 않고 태그 이름만 문자열로 받는다 — 엔진이
 * Vanity의 concrete 타입에 의존하지 않기 위함이다.
 *
 * <p><b>coverageDays(2026-08-20 확정)</b>: {@link ProductCandidateInput}과 동일한 개념 — 두 제품의
 * 기준일 중 더 늦은 날짜(또는 usageTimingChangedAt으로 보정된 날짜)부터 시작하는 관측 window 안에서
 * 실제 CheckIn이 존재한 날짜 수. 0이면 {@link ExclusionReason#INSUFFICIENT_RECORDS}로 제외한다.
 * 조합 자체의 reference date는 window/coverage 계산에만 쓰이고 엔진 입력에는 남기지 않는다 — 조합의
 * 근거 강도/tie-break는 {@code conflictPlacement}만으로 결정되기 때문이다(기존 규칙 그대로).
 *
 * @param tagA               충돌 태그 A
 * @param tagB               충돌 태그 B
 * @param conflictPlacement  충돌 배치 방식(같은 time slot / AM-PM 분리 / 충돌 없음)
 * @param coverageDays       관측 window 안에서 실제 CheckIn이 존재한 날짜 수
 */
public record CombinationCandidateInput(
		String tagA,
		String tagB,
		ConflictPlacement conflictPlacement,
		long coverageDays
) {
}
