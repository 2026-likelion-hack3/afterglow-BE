package com.afterglow.domain.episode.analysis.domain;

/** 제품 조합 후보의 근거 — 충돌 태그 쌍과 실제 배치 방식. */
public record CombinationEvidence(String tagA, String tagB, ConflictPlacement conflictPlacement) implements Evidence {
}
