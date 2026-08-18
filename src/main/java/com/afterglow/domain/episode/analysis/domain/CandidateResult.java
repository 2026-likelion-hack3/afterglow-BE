package com.afterglow.domain.episode.analysis.domain;

/**
 * 근거 강도까지 계산된 후보 하나.
 *
 * @param coverageDays 이 후보의 {@link RecordCoverage#coverageDays}(최초 기록일~분석 기준일 calendar-day
 *                     커버리지) — card가 "N일치 기록에 근거" 표시에 쓴다. {@link FrequencyEvidence}의
 *                     관찰 횟수와는 다른 값이라 섞지 않는다. <b>SLEEP/WEATHER만 실제 값을 갖고, PRODUCT/
 *                     COMBINATION은 항상 null이다</b> — 7일 coverage 게이트가 수면/날씨에만 적용되고
 *                     (Manyfast 통합 분석 F-ZSPZHH updateData 확정, 2026-08-18) 제품/조합의 coverage
 *                     시작점은 기획에 정의돼 있지 않아, 의미 없는 날짜를 임의로 채우지 않는다.
 */
public record CandidateResult(CandidateType type, EvidenceStrength strength, Evidence evidence, Long coverageDays) {

	public int score() {
		return strength.score();
	}
}
