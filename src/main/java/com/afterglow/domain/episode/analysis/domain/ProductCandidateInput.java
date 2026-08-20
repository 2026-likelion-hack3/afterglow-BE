package com.afterglow.domain.episode.analysis.domain;

import java.time.LocalDate;

/**
 * "특정 제품" 후보 하나(예: 최근 새로 쓰기 시작한 제품 1개)를 엔진이 판단할 수 있을 만큼 정규화한 입력.
 * Vanity의 실제 Product 엔티티 구조를 그대로 옮기지 않고, 엔진이 필요로 하는 최소 필드만 둔다.
 *
 * <p><b>coverageDays(2026-08-20 확정)</b>: {@code referenceCertainty}/window 안에서 실제 CheckIn이
 * 존재한 날짜 수 — calendar-day 길이가 아니다({@link ProductObservationWindow} 참고). 0이면 엔진이
 * {@link ExclusionReason#INSUFFICIENT_RECORDS}로 제외한다. 이전에는 제품/조합에 coverage 개념 자체가
 * 없었으나(2026-08-18), Manyfast 후속 확정으로 다시 생겼다.
 *
 * <p><b>referenceCertainty(2026-08-20 확정)</b>: Vanity가 정확한 사용 시작일을 주지 않고 "개봉 시기" 범주
 * (OpeningPeriod)만 주므로 {@code usageStartDate}는 항상 추정값이다 — {@link ReferenceCertainty#ESTIMATED}면
 * 근거 강도를 한 단계 낮추고, {@link ReferenceCertainty#FALLBACK}(개봉 시기 자체가 없어 등록일로 대체)이면
 * 강도를 WEAK로 고정한다.
 *
 * <p><b>symptomStartDate(2026-08-20 확정)</b>: {@code Intake.onsetPeriod}(오늘/2~3일 전/1주 전/2주 이상)를
 * 실제 날짜로 환산한 값 — Manyfast 기획 확정에 따라 TODAY=분석 기준일, 2~3일 전=3일 전, 1주 전=7일 전,
 * 2주 이상=14일 전이다(환산 로직: {@code EpisodeAnalysisService.symptomStartDateOf}). 이전에는 이 환산
 * 규칙이 없어 TODAY가 아니면 신뢰할 수 없는 값으로 보고 강도를 WEAK로 고정하는 별도 플래그
 * (symptomStartDateReliable)가 있었으나, 규칙이 확정되며 제거했다 — 이제 모든 onsetPeriod 값이 실제 날짜로
 * timing 계산에 그대로 쓰인다.
 *
 * @param productId                     제품 식별값
 * @param usageStartDate                제품 사용 시작일(추정값 포함)
 * @param symptomStartDate              증상 시작일(에피소드 기준, 모든 제품 후보가 동일 값을 공유)
 * @param changedProductCountInWindow   같은 기간(증상 시작 전후) 동안 사용을 시작한 제품 수 — <b>이 후보
 *                                      자신을 포함한 총 개수</b>다. "다른 제품 변경 없음"이면 자기 자신만
 *                                      해당하므로 1이 된다(예: 이 제품 하나만 새로 썼으면 1, 이 제품을 포함해
 *                                      2개를 동시에 새로 썼으면 2). 이 값을 직접 비교하지 말고
 *                                      {@link #hasOtherProductChangesInWindow()}를 사용한다.
 * @param referenceCertainty            usageStartDate가 추정값인지, 추정조차 못해 등록일로 대체했는지
 * @param coverageDays                  관측 window 안에서 실제 CheckIn이 존재한 날짜 수
 */
public record ProductCandidateInput(
		Long productId,
		LocalDate usageStartDate,
		LocalDate symptomStartDate,
		int changedProductCountInWindow,
		ReferenceCertainty referenceCertainty,
		long coverageDays
) {
	/** "같은 기간 다른 제품 변경 없음" 여부 — changedProductCountInWindow가 자기 자신 포함 값이므로 1 초과일 때만 true다. */
	public boolean hasOtherProductChangesInWindow() {
		return changedProductCountInWindow > 1;
	}
}
