package com.afterglow.domain.episode.analysis.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResult;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResultRepository;

public interface EpisodeAnalysisResultJpaRepository
		extends JpaRepository<EpisodeAnalysisResult, Long>, EpisodeAnalysisResultRepository {
}
