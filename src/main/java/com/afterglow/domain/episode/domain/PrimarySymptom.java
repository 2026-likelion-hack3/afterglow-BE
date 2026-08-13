package com.afterglow.domain.episode.domain;

/**
 * 증상 극좌표 각도가 매핑되는 증상 구간 — 기능명세서 2.1.
 * 표시 문구는 "6개 항목"이라 되어 있으나 데이터 정의는 5개로 명시되어 있어 5개 기준으로 구현한다.
 * 이 불일치는 기획 확인 대상이다.
 */
public enum PrimarySymptom {
	DRYNESS_TIGHTNESS,
	ITCHING,
	STINGING,
	REDNESS,
	TROUBLE
}
