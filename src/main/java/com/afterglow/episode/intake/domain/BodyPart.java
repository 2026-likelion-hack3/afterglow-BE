package com.afterglow.episode.intake.domain;

/** 고정 문진 2번 — 증상 부위, 복수 선택 가능. 단 WHOLE_FACE는 다른 값과 동시 선택 불가(Episode.submitIntake에서 검증). */
public enum BodyPart {
	FOREHEAD,
	EYE_AREA,
	CHEEK,
	AROUND_MOUTH,
	CHIN,
	WHOLE_FACE
}
