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

	/** E1(기록 목록)/E3(여러 회차 요약) 최신순 조회용 — 동시각이면 id 내림차순으로 tie-break한다. */
	List<Episode> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);

	void deleteByAccountId(Long accountId);
}
