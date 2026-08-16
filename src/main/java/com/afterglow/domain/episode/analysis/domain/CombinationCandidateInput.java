package com.afterglow.domain.episode.analysis.domain;

/**
 * "제품 조합" 후보 하나(충돌 태그를 가진 제품 쌍)를 엔진이 판단할 수 있을 만큼 정규화한 입력.
 * Vanity의 {@code InteractionTag} enum을 직접 참조하지 않고 태그 이름만 문자열로 받는다 — 엔진이
 * Vanity의 concrete 타입에 의존하지 않기 위함이다.
 *
 * @param tagA               충돌 태그 A
 * @param tagB               충돌 태그 B
 * @param conflictPlacement  충돌 배치 방식(같은 time slot / AM-PM 분리 / 충돌 없음)
 * @param coverage           데이터 coverage 정보(7일 게이트용)
 */
public record CombinationCandidateInput(
		String tagA,
		String tagB,
		ConflictPlacement conflictPlacement,
		RecordCoverage coverage
) {
}
