package com.afterglow.domain.episode.analysis.domain;

/**
 * "수면"/"날씨" 후보에 공통으로 쓰는 빈도 기반 입력. Tracking의 실제 기록 구조를 추측해서 옮기지 않고,
 * 엔진이 필요로 하는 관측 횟수/일치 횟수만 둔다.
 *
 * @param observationCount         전체 관측 횟수
 * @param matchedObservationCount  증상과 일치한 관측 횟수
 * @param coverage                 데이터 coverage 정보(7일 게이트용)
 */
public record ObservationCandidateInput(
		int observationCount,
		int matchedObservationCount,
		RecordCoverage coverage
) {
	public double matchRatio() {
		return observationCount == 0 ? 0.0 : (double) matchedObservationCount / observationCount;
	}
}
