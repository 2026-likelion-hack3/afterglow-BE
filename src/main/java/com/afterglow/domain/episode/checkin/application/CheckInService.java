package com.afterglow.domain.episode.checkin.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.checkin.domain.CheckIn;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckInService {

	private final CheckInRepository checkInRepository;
	private final EpisodeRepository episodeRepository;

	/**
	 * 최초 기록이면 row를 새로 만들고, 같은 날 재기록이면 마지막 응답으로 덮어쓴다(find-or-create +
	 * dirty checking). 조회/생성/변경이 하나의 persistence context 안에서 이어져야 하므로 @Transactional이 필요하다.
	 */
	@Transactional
	public CheckIn record(Long accountId, Long episodeId, LocalDate checkInDate, CheckInStatus status) {
		requireEpisodeOwnership(accountId, episodeId);
		CheckIn checkIn = checkInRepository.findByEpisodeIdAndCheckInDate(episodeId, checkInDate)
				.orElseGet(() -> checkInRepository.save(CheckIn.create(episodeId, checkInDate, status)));
		checkIn.overwrite(status);
		return checkIn;
	}

	/** episode 소유 확인 + 단건 목록 조회, 둘 다 단순 SELECT뿐이고 그 사이 원자성이 필요한 변경이 없어 @Transactional을 두지 않는다. */
	public List<CheckIn> getAll(Long accountId, Long episodeId) {
		requireEpisodeOwnership(accountId, episodeId);
		return checkInRepository.findByEpisodeIdOrderByCheckInDateAsc(episodeId);
	}

	private void requireEpisodeOwnership(Long accountId, Long episodeId) {
		episodeRepository.findByIdAndAccountId(episodeId, accountId)
				.orElseThrow(() -> new NotFoundException("에피소드를 찾을 수 없습니다."));
	}
}
