package com.afterglow.domain.episode.analysis.domain;

/** 수면/날씨 후보의 빈도형 근거 — 전체 관측 횟수와 일치 횟수. */
public record FrequencyEvidence(int observationCount, int matchedObservationCount) implements Evidence {
}
