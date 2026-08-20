package com.afterglow.domain.episode.analysis.domain;

import java.util.Optional;

public interface EpisodeAnalysisResultRepository {

	EpisodeAnalysisResult save(EpisodeAnalysisResult result);

	Optional<EpisodeAnalysisResult> findByEpisodeIdAndAccountId(Long episodeId, Long accountId);
}
