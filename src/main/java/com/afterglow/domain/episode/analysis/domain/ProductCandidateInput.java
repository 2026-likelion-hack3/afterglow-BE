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
 * <p><b>symptomStartDateReliable(2026-08-20 신설, 후속 기획 확정 대기)</b>: {@code symptomStartDate}가
 * 실제 증상 시작일인지, 아니면 "정확한 날짜를 모른다"는 뜻의 임시값(현재는 분석 기준일을 그대로 씀)인지.
 * Intake 1번 문항(onsetPeriod: 오늘/2~3일 전/1주 전/2주 이상)을 실제 날짜로 환산하는 규칙이 아직
 * Manyfast에 없어서(신규 날짜 환산을 임의로 발명하지 않음), 오늘(TODAY) 응답일 때만 true다. false면
 * timing 계산 자체를 신뢰할 수 없으므로(예: 실제로는 증상이 시작된 뒤에 쓰기 시작한 제품이 "증상 전에
 * 시작"한 것처럼 잘못 판정될 수 있음 — analysisDate가 항상 실제 증상 시작일보다 같거나 늦기 때문) 강도를
 * WEAK로 고정한다. onsetPeriod → 날짜 환산 규칙이 기획에서 확정되면 이 필드를 없애고 정확한
 * {@code symptomStartDate}를 직접 계산하면 된다.
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
 * @param symptomStartDateReliable      symptomStartDate가 실제 증상 시작일인지(onsetPeriod=TODAY만 true)
 * @param coverageDays                  관측 window 안에서 실제 CheckIn이 존재한 날짜 수
 */
public record ProductCandidateInput(
		Long productId,
		LocalDate usageStartDate,
		LocalDate symptomStartDate,
		int changedProductCountInWindow,
		ReferenceCertainty referenceCertainty,
		boolean symptomStartDateReliable,
		long coverageDays
) {
	/** "같은 기간 다른 제품 변경 없음" 여부 — changedProductCountInWindow가 자기 자신 포함 값이므로 1 초과일 때만 true다. */
	public boolean hasOtherProductChangesInWindow() {
		return changedProductCountInWindow > 1;
	}
}
