package com.afterglow.domain.episode.analysis.domain;

/**
 * 공통 gate에서 제외된 후보 하나. 제외된 후보는 {@link AnalysisResult#candidates()}에 들어가지 않고
 * 이 목록으로만 남는다 — "대상이 아예 없었다"와 "기록이 부족했다"를 downstream(card 등)이 구분할 수 있게 하기 위함이다.
 *
 * @param type                제외된 후보 타입
 * @param reason              제외 사유
 * @param candidateIdentifier 여러 후보가 있을 수 있는 타입(제품/조합)에서 어떤 후보인지 식별하는 값.
 *                            타입당 후보가 최대 하나뿐인 수면/날씨, 그리고 NO_TARGET(식별할 대상 자체가 없음)에서는 null.
 */
public record CandidateExclusion(CandidateType type, ExclusionReason reason, String candidateIdentifier) {

	public CandidateExclusion(CandidateType type, ExclusionReason reason) {
		this(type, reason, null);
	}
}
