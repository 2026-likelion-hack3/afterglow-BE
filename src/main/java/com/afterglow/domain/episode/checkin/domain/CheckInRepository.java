package com.afterglow.domain.episode.checkin.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository {

	CheckIn save(CheckIn checkIn);

	Optional<CheckIn> findByEpisodeIdAndCheckInDate(Long episodeId, LocalDate checkInDate);

	List<CheckIn> findByEpisodeIdOrderByCheckInDateAsc(Long episodeId);
}
