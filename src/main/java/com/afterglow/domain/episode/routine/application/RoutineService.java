package com.afterglow.domain.episode.routine.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.checkin.domain.CheckIn;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.checkin.domain.Day3JudgmentEngine;
import com.afterglow.domain.episode.checkin.domain.Day3JudgmentInput;
import com.afterglow.domain.episode.checkin.domain.Day3JudgmentResult;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.EpisodeStatus;
import com.afterglow.domain.episode.routine.domain.ProductUsageEvent;
import com.afterglow.domain.episode.routine.domain.Routine;
import com.afterglow.domain.episode.routine.domain.RoutineCreateInput;
import com.afterglow.domain.episode.routine.domain.RoutinePlanner;
import com.afterglow.domain.episode.routine.domain.RoutineProductLifecycle;
import com.afterglow.domain.episode.routine.domain.RoutineRepository;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.afterglow.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Routine 시작/조회 및 CheckIn·{@link Day3JudgmentEngine} 연결을 담당하는 application 계층.
 *
 * <p>Routine 생성 전제조건("결과 카드가 생성된 상태")을 그대로 검증할 방법이 없다 — Analysis/Card는 DB에
 * 저장하지 않는 순수 함수라(docs/domains/episode.md) Episode 쪽에 "카드까지 생성됨"을 나타내는 상태가
 * 없다. 대신 Episode에 실제로 남아 있는 가장 가까운 신호인 {@code INTAKE_COMPLETED}로 대체한다 — 진짜
 * 전제조건은 Card/Analysis가 영속화되는 시점에 다시 좁혀야 한다(BLOCKED, 보고서 참고).
 */
@Service
@RequiredArgsConstructor
public class RoutineService {

	private final RoutineRepository routineRepository;
	private final EpisodeRepository episodeRepository;
	private final CheckInRepository checkInRepository;
	private final RoutinePlanner routinePlanner = new RoutinePlanner();
	private final Day3JudgmentEngine day3JudgmentEngine = new Day3JudgmentEngine();
	private final RoutineProductLifecycle routineProductLifecycle = new RoutineProductLifecycle();

	@Transactional
	public Routine start(Long accountId, Long episodeId, RoutineCreateInput input) {
		Episode episode = requireOwnedEpisode(accountId, episodeId);
		if (episode.getStatus() != EpisodeStatus.INTAKE_COMPLETED) {
			throw new AfterglowException(ErrorCode.INVALID_EPISODE_STATE);
		}
		if (routineRepository.existsByEpisodeId(episodeId)) {
			throw new AfterglowException(ErrorCode.INVALID_EPISODE_STATE, "이미 이 에피소드의 루틴이 존재합니다.");
		}

		Routine routine = routinePlanner.plan(episodeId, accountId, LocalDate.now(), input);
		try {
			return routineRepository.save(routine);
		} catch (DataIntegrityViolationException e) {
			// existsByEpisodeId 체크 이후 동시에 생성 요청이 들어온 race를 unique 제약(uk_routine_episode)이 최종 방어한다.
			throw new AfterglowException(ErrorCode.INVALID_EPISODE_STATE, "이미 이 에피소드의 루틴이 존재합니다.");
		}
	}

	public Routine get(Long accountId, Long episodeId) {
		requireOwnedEpisode(accountId, episodeId);
		return routineRepository.findByEpisodeIdAndAccountId(episodeId, accountId)
				.orElseThrow(() -> new NotFoundException("루틴을 찾을 수 없습니다."));
	}

	/**
	 * 저장된 Routine의 시작일로 Day1/Day2/Day3 날짜를 계산해 그 날짜의 CheckIn을 찾고
	 * {@link Day3JudgmentEngine}에 그대로 넘긴다 — Day3 판정 로직 자체는 재구현하지 않는다.
	 */
	public Day3JudgmentResult judgeDay3(Long accountId, Long episodeId) {
		Routine routine = get(accountId, episodeId);
		Day3JudgmentInput judgmentInput = new Day3JudgmentInput(
				statusOn(routine, 1),
				statusOn(routine, 2),
				statusOn(routine, 3)
		);
		return day3JudgmentEngine.judge(judgmentInput);
	}

	/**
	 * 계정의 전체 Routine 이력(여러 Episode에 걸칠 수 있다)에서 이 제품의 중단/재개 사건을 도출한다 —
	 * Vanity를 호출하지 않고 저장된 Routine만 읽는다. 재개 여부/재개일까지는 알려주지만, 그걸 보고 Analysis
	 * 기준일을 갱신할지 말지는 이 메서드의 책임이 아니다(기획 확정: 앱이 중단시킨 뒤 재개해도 기준일을
	 * 갱신하지 않는다 — Analysis 통합 시점에 그 규칙을 지켜야 한다).
	 */
	public List<ProductUsageEvent> productLifecycle(Long accountId, Long productId) {
		List<Routine> routines = routineRepository.findByAccountIdOrderByStartDateAscIdAsc(accountId);
		return routineProductLifecycle.historyOf(routines, productId);
	}

	private CheckInStatus statusOn(Routine routine, int dayNumber) {
		return checkInRepository.findByEpisodeIdAndCheckInDate(routine.getEpisodeId(), routine.dateOf(dayNumber))
				.map(CheckIn::getStatus)
				.orElse(null);
	}

	private Episode requireOwnedEpisode(Long accountId, Long episodeId) {
		return episodeRepository.findByIdAndAccountId(episodeId, accountId)
				.orElseThrow(() -> new NotFoundException("에피소드를 찾을 수 없습니다."));
	}
}
