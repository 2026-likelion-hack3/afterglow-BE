package com.afterglow.domain.episode.domain;

import java.util.Optional;

public interface EpisodeRepository {

	Episode save(Episode episode);

	Optional<Episode> findByIdAndAccountId(Long id, Long accountId);
}
