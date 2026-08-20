package com.afterglow.domain.episode.checkin.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.checkin.domain.CheckIn;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.domain.Episode;
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

	/**
	 * 계정의 여러 Episode에 걸친 CheckIn 날짜 집합(from~to, 둘 다 포함) — Analysis가 Product/Combination
	 * observation window의 coverageDays(실제 CheckIn이 존재한 날짜 수)를 계산할 때 쓰는 read-only 조회.
	 * 상태값(IMPROVED/SAME/WORSE)은 이 용도에서 쓰지 않아 날짜만 반환한다.
	 */
	public Set<LocalDate> findCheckInDatesByAccountAndPeriod(Long accountId, LocalDate from, LocalDate to) {
		return findCheckInsByAccountAndPeriod(accountId, from, to).stream()
				.map(CheckIn::getCheckInDate)
				.collect(Collectors.toSet());
	}

	/**
	 * 계정의 여러 Episode에 걸친 날짜→상태 맵(from~to, 둘 다 포함) — Sleep/Weather observation의
	 * "그날 증상이 좋아졌는지/나빠졌는지" source of truth로 쓰는 read-only 조회. 같은 날짜에 서로 다른
	 * Episode의 CheckIn이 겹치면(드묾) 마지막으로 처리된 값이 남는다.
	 */
	public Map<LocalDate, CheckInStatus> findCheckInStatusesByAccountAndPeriod(Long accountId, LocalDate from, LocalDate to) {
		return findCheckInsByAccountAndPeriod(accountId, from, to).stream()
				.collect(Collectors.toMap(CheckIn::getCheckInDate, CheckIn::getStatus, (a, b) -> b));
	}

	private List<CheckIn> findCheckInsByAccountAndPeriod(Long accountId, LocalDate from, LocalDate to) {
		List<Long> episodeIds = episodeRepository.findByAccountId(accountId).stream().map(Episode::getId).toList();
		if (episodeIds.isEmpty()) {
			return List.of();
		}
		return checkInRepository.findByEpisodeIdInAndCheckInDateBetween(episodeIds, from, to);
	}

	private void requireEpisodeOwnership(Long accountId, Long episodeId) {
		episodeRepository.findByIdAndAccountId(episodeId, accountId)
				.orElseThrow(() -> new NotFoundException("에피소드를 찾을 수 없습니다."));
	}
}
