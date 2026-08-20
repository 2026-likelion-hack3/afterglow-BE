package com.afterglow.domain.episode.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResult;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResultRepository;
import com.afterglow.domain.episode.checkin.domain.CheckIn;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.checkin.domain.Day3Verdict;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.intake.api.EpisodeSummaryResponse;
import com.afterglow.domain.episode.routine.application.RoutineService;
import com.afterglow.domain.episode.routine.domain.Routine;
import com.afterglow.domain.episode.routine.domain.RoutineItem;
import com.afterglow.domain.episode.routine.domain.RoutineRepository;

import lombok.RequiredArgsConstructor;

/**
 * Figma E1(기록 목록) 최소 조회 — 새로운 판정/집계 알고리즘을 만들지 않고, 이미 존재하는 Episode/
 * EpisodeAnalysisResult/Routine/CheckIn 데이터를 단순 조회해 조합만 한다. Day3 verdict는 기존
 * {@link RoutineService#judgeDay3}를 그대로 재사용한다(재구현 아님) — Day1~3 CheckIn 상태 조회만
 * 같은 방식(routine.dateOf(dayNumber) → 그 날짜의 CheckIn)으로 별도 조회한다(judgeDay3 내부의
 * private 로직을 건드리지 않기 위해 그대로 다시 조회하는 쪽을 택함, 기존 코드 변경 최소화).
 */
@Service
@RequiredArgsConstructor
public class EpisodeSummaryService {

	private final EpisodeRepository episodeRepository;
	private final EpisodeAnalysisResultRepository episodeAnalysisResultRepository;
	private final RoutineRepository routineRepository;
	private final RoutineService routineService;
	private final CheckInRepository checkInRepository;

	/** 본인 계정의 Episode만, 최신순. Analysis/Routine/Day3 결과가 아직 없으면 해당 필드는 null/빈 배열로 정상 반환한다. */
	public List<EpisodeSummaryResponse> getEpisodes(Long accountId) {
		return episodeRepository.findByAccountIdOrderByCreatedAtDescIdDesc(accountId).stream()
				.map(episode -> toSummary(accountId, episode))
				.toList();
	}

	private EpisodeSummaryResponse toSummary(Long accountId, Episode episode) {
		EpisodeAnalysisResult analysis = episodeAnalysisResultRepository
				.findByEpisodeIdAndAccountId(episode.getId(), accountId)
				.orElse(null);

		Optional<Routine> routineOpt = routineRepository.findByEpisodeIdAndAccountId(episode.getId(), accountId);

		CheckInStatus day1 = null;
		CheckInStatus day2 = null;
		CheckInStatus day3 = null;
		Day3Verdict verdict = null;
		List<Long> productIds = List.of();

		if (routineOpt.isPresent()) {
			Routine routine = routineOpt.get();
			day1 = statusOn(routine, 1);
			day2 = statusOn(routine, 2);
			day3 = statusOn(routine, 3);
			verdict = routineService.judgeDay3(accountId, episode.getId()).verdict();
			productIds = routine.getItems().stream().map(RoutineItem::getProductId).distinct().toList();
		}

		return new EpisodeSummaryResponse(
				episode.getId(),
				episode.getCreatedAt(),
				episode.getSymptom().getPrimarySymptom(),
				episode.getStatus(),
				analysis != null ? analysis.isHold() : null,
				analysis != null ? analysis.getCauseType() : null,
				analysis != null ? analysis.getHoldReason() : null,
				analysis != null ? analysis.getConfidence() : null,
				routineOpt.map(Routine::getStatus).orElse(null),
				routineOpt.map(Routine::getStartDate).orElse(null),
				day1, day2, day3,
				verdict,
				productIds
		);
	}

	private CheckInStatus statusOn(Routine routine, int dayNumber) {
		return checkInRepository.findByEpisodeIdAndCheckInDate(routine.getEpisodeId(), routine.dateOf(dayNumber))
				.map(CheckIn::getStatus)
				.orElse(null);
	}
}
