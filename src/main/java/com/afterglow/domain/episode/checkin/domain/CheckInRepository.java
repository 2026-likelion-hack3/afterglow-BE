package com.afterglow.domain.episode.checkin.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository {

	CheckIn save(CheckIn checkIn);

	Optional<CheckIn> findByEpisodeIdAndCheckInDate(Long episodeId, LocalDate checkInDate);

	List<CheckIn> findByEpisodeIdOrderByCheckInDateAsc(Long episodeId);

	/** Analysis의 coverage 계산 전용 — 계정의 여러 Episode에 걸친 CheckIn을 기간으로 조회한다(둘 다 포함). */
	List<CheckIn> findByEpisodeIdInAndCheckInDateBetween(Collection<Long> episodeIds, LocalDate from, LocalDate to);

	void deleteByEpisodeIdIn(Collection<Long> episodeIds);
}
