package com.afterglow.domain.episode.analysis.domain;

import java.time.LocalDate;

/**
 * "특정 제품" 후보 하나(예: 최근 새로 쓰기 시작한 제품 1개)를 엔진이 판단할 수 있을 만큼 정규화한 입력.
 * Vanity의 실제 Product 엔티티 구조를 그대로 옮기지 않고, 엔진이 필요로 하는 최소 필드만 둔다.
 *
 * <p><b>coverage/7일 게이트 없음(2026-08-18 확정)</b>: Manyfast 통합 분석(F-ZSPZHH, updateData)
 * "[후보 수집]" 규칙 원문 — "일일 기록이 7일 미만이면 <b>수면과 날씨를</b> 제외하고 사유를 기록부족으로
 * 남긴다" — 이 7일 게이트는 수면/날씨에만 명시돼 있고 제품에는 적용되지 않는다. 그래서 이 입력에는
 * coverage 필드가 없다 — 대상(제품)이 존재하면 게이트 없이 바로 근거 강도를 계산한다.
 *
 * <p><b>usageStartDate의 실제 source는 Pending</b>: Vanity의 {@code Product}에는 "개봉일"(openedAt)만
 * 있고, Manyfast에 "개봉일 = 사용 시작일"이라는 대응이 정의돼 있지 않다. 이 필드의 개념 자체는 유지하되,
 * 실제 운영에서 어떤 Vanity 데이터로 채울지는 기획 확인 전까지 정하지 않는다.
 *
 * @param productId                     제품 식별값
 * @param usageStartDate                제품 사용 시작일
 * @param symptomStartDate              증상 시작일(에피소드 기준, 모든 제품 후보가 동일 값을 공유)
 * @param changedProductCountInWindow   같은 기간(증상 시작 전후) 동안 사용을 시작한 제품 수 — <b>이 후보
 *                                      자신을 포함한 총 개수</b>다. "다른 제품 변경 없음"이면 자기 자신만
 *                                      해당하므로 1이 된다(예: 이 제품 하나만 새로 썼으면 1, 이 제품을 포함해
 *                                      2개를 동시에 새로 썼으면 2). 이 값을 직접 비교하지 말고
 *                                      {@link #hasOtherProductChangesInWindow()}를 사용한다.
 */
public record ProductCandidateInput(
		Long productId,
		LocalDate usageStartDate,
		LocalDate symptomStartDate,
		int changedProductCountInWindow
) {
	/** "같은 기간 다른 제품 변경 없음" 여부 — changedProductCountInWindow가 자기 자신 포함 값이므로 1 초과일 때만 true다. */
	public boolean hasOtherProductChangesInWindow() {
		return changedProductCountInWindow > 1;
	}
}
