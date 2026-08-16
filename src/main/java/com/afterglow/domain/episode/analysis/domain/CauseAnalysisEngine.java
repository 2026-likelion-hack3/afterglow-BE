package com.afterglow.domain.episode.analysis.domain;

/**
 * 원인 후보 판정 계약. 입력 {@link AnalysisInput}만으로 결정적인 {@link AnalysisResult}를 산출해야
 * 하며, Repository나 다른 도메인 서비스를 호출하지 않는다 — 실제 데이터 조회는 이 인터페이스를 호출하는
 * 쪽(향후 EpisodeAnalysisService)의 책임이다.
 */
public interface CauseAnalysisEngine {

	AnalysisResult analyze(AnalysisInput input);
}
