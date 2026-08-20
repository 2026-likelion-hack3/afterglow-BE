package com.afterglow.domain.episode.card.domain;

/**
 * 판단 보류 화면 문구를 고를 때 쓰는 사유 — Manyfast 통합 분석(updateData) exceptions: "보류 화면 문구는
 * 제외 사유를 보고 고른다. 기록부족이면 며칠 더 모이면 알려드릴 수 있다고 말하고, 대상없음이면 그런
 * 약속을 하지 않는다."
 */
public enum ResultCardHoldReason {
	/** 후보 자체가 없었음(모든 타입이 {@code ExclusionReason.NO_TARGET}) — 재평가 약속을 하지 않는다. */
	NO_TARGET,
	/** 대상은 있었지만 기록이 부족해 제외됨(하나 이상 {@code ExclusionReason.INSUFFICIENT_RECORDS}) —
	 *  기록이 더 모이면 알려줄 수 있다는 취지로 안내한다. */
	INSUFFICIENT_RECORDS,
	/** 후보는 있었지만 근거가 약하거나(top WEAK) 우열을 가릴 수 없어(same-type tie) 또는 gap이 작아 보류. */
	INCONCLUSIVE_EVIDENCE
}
