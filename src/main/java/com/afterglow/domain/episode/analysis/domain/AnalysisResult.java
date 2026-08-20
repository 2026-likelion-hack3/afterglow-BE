package com.afterglow.domain.episode.analysis.domain;

import java.util.List;

/**
 * {@link CauseAnalysisEngine}의 산출물.
 *
 * <p>{@code candidates}는 7일 게이트를 통과해 근거 강도까지 계산된 후보를 점수 내림차순(동점은
 * {@link CandidateType} 우선순위)으로 정렬한 목록이다 — 3일차 "다음 원인 후보"가 재조회 없이 순서를
 * 그대로 쓸 수 있게 하기 위함이다. <b>{@code candidates.get(0)}은 정렬상 1번째일 뿐, "선택된 원인"이라는
 * 뜻이 아니다</b> — same-type tie로 HOLD가 된 경우에도 정렬은 되어 있지만 그중 하나가 골라진 것은 아니다.
 * "최종 선택된 원인 후보"를 뜻하는 필드는 {@code topCandidate} 하나뿐이다.
 *
 * <p>{@code exclusions}는 공통 gate에서 제외된 후보의 사유(대상 없음/기록 부족)를 담는다. 제외된 후보는
 * {@code candidates}에 들어가지 않는다 — 정상 판정 후보와 제외 사유를 섞지 않는다.
 *
 * <p>HOLD가 아니면 {@code topCandidate}/{@code confidence}가 반드시 채워지고, HOLD면 반드시 둘 다
 * null이다 — 이 불변식은 compact constructor에서 강제한다. 확신 단계는 HOLD가 아닌 결과에만 적용되는
 * 개념이라 "HOLD인데 confidence가 존재하는" 상태 자체를 만들 수 없게 한다.
 */
public record AnalysisResult(
		List<CandidateResult> candidates,
		List<CandidateExclusion> exclusions,
		boolean hold,
		CandidateResult topCandidate,
		Confidence confidence
) {
	public AnalysisResult {
		candidates = List.copyOf(candidates);
		exclusions = List.copyOf(exclusions);
		if (hold && (topCandidate != null || confidence != null)) {
			throw new IllegalStateException("HOLD 결과는 topCandidate/confidence를 가질 수 없다.");
		}
		if (!hold && (topCandidate == null || confidence == null)) {
			throw new IllegalStateException("HOLD가 아닌 결과는 topCandidate와 confidence가 반드시 있어야 한다.");
		}
	}

	public static AnalysisResult hold(List<CandidateResult> candidates, List<CandidateExclusion> exclusions) {
		return new AnalysisResult(candidates, exclusions, true, null, null);
	}

	public static AnalysisResult determined(
			List<CandidateResult> candidates, List<CandidateExclusion> exclusions,
			CandidateResult topCandidate, Confidence confidence) {
		return new AnalysisResult(candidates, exclusions, false, topCandidate, confidence);
	}
}
