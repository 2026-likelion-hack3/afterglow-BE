package com.afterglow.domain.episode.analysis.domain;

import java.time.LocalDate;

/** 특정 제품 후보의 시점형 근거 — 제품 사용 시작일과 증상 시작일. */
public record TimingEvidence(Long productId, LocalDate usageStartDate, LocalDate symptomStartDate) implements Evidence {
}
