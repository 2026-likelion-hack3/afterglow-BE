package com.afterglow.domain.episode.analysis.domain;

/**
 * 7일 게이트를 통과해 근거 강도까지 계산된 후보 하나.
 *
 * @param coverageDays 이 후보의 {@link RecordCoverage#coverageDays}(최초 기록일~분석 기준일 calendar-day
 *                     커버리지) — card가 "N일치 기록에 근거" 표시에 쓴다. {@link FrequencyEvidence}의
 *                     관찰 횟수와는 다른 값이라 섞지 않는다.
 */
public record CandidateResult(CandidateType type, EvidenceStrength strength, Evidence evidence, long coverageDays) {

	public int score() {
		return strength.score();
	}
}
