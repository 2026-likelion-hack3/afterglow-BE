package com.afterglow.domain.episode.analysis.domain;

/** 제품 조합 후보에서 충돌 태그 두 개가 실제로 배치된 방식. */
public enum ConflictPlacement {
	/** 같은 time slot(아침 또는 저녁)에 함께 배치 — 강. */
	SAME_TIME_SLOT,
	/** 충돌은 있지만 아침/저녁으로 분리 배치 — 중. */
	SPLIT_AM_PM,
	/** 충돌 태그 조합 자체가 없음 — 약. */
	NONE
}
