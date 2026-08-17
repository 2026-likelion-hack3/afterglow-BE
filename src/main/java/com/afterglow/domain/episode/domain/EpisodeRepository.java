package com.afterglow.domain.episode.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EpisodeRepository {

	Episode save(Episode episode);

	Optional<Episode> findByIdAndAccountId(Long id, Long accountId);

	List<Episode> findByAccountId(Long accountId);

	List<Episode> findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
			Long accountId, LocalDateTime from, LocalDateTime toExclusive);

	void deleteByAccountId(Long accountId);
}
