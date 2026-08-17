package com.afterglow.domain.episode.checkin.domain;

/** 3일 관찰 기간의 최종 판정 — 기능명세서 4.2(Manyfast F-TWLPPZ). */
public enum Day3Verdict {
	/** 응답이 2일 이하라 판정하지 않음("판단하기 이릅니다"). */
	WITHHELD,
	/** 좋아졌다가 우세 — 루틴 유지. */
	MAINTAIN,
	/** 비슷하다가 우세 — 연장 또는 다음 원인 후보 검토(카드/분석 영역, 이 엔진은 EXTEND까지만 판정). */
	EXTEND,
	/** 나빠졌다가 있었음(1~2일차 즉시 발생 포함) — 중단. */
	STOP
}
