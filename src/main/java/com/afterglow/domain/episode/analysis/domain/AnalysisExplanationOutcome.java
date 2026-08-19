package com.afterglow.domain.episode.analysis.domain;

/** {@link EpisodeAnalysisResultRepository}로 조회한 결과를 설명한 최종 산출물 — 실제 source(AI/FALLBACK)를 함께 담는다. */
public record AnalysisExplanationOutcome(AnalysisExplanation explanation, ExplanationSource source) {
}
