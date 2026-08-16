package com.afterglow.domain.episode.analysis.domain;

/** 후보 유형별 근거 객체. 유형마다 구조가 달라 공용 필드로 묶지 않고 sealed 타입으로 구분한다. */
public sealed interface Evidence permits TimingEvidence, CombinationEvidence, FrequencyEvidence {
}
