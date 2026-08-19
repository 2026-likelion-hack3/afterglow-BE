package com.afterglow.domain.episode.analysis.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.analysis.domain.AnalysisInput;
import com.afterglow.domain.episode.analysis.domain.AnalysisResult;
import com.afterglow.domain.episode.analysis.domain.CauseAnalysisEngine;
import com.afterglow.domain.episode.analysis.domain.CombinationCandidateInput;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResult;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResultRepository;
import com.afterglow.domain.episode.analysis.domain.ObservationCandidateInput;
import com.afterglow.domain.episode.analysis.domain.ProductCandidateInput;
import com.afterglow.domain.episode.analysis.domain.RuleBasedCauseAnalysisEngine;
import com.afterglow.domain.episode.card.domain.ResultCardAssembler;
import com.afterglow.domain.episode.card.domain.ResultCardResult;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.EpisodeStatus;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.afterglow.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * 2.4 통합 분석 orchestration — account ownership 확인 → Episode 상태 확인 → raw data 조회 →
 * {@link RuleBasedCauseAnalysisEngine} 실행 → {@link ResultCardAssembler}로 카드 조립 → 저장 → 반환.
 * 이미 {@code ANALYZED}면 재실행하지 않고 저장된 결과를 그대로 돌려준다(idempotent, 기존 설계 그대로).
 *
 * <p><b>Vanity/Tracking 실제 연동 없음(2026-08-19 기준)</b>: 두 도메인 모두 Repository/조회 기능이 아직
 * 없어(Vanity는 Entity만, Tracking은 패키지만 존재) {@code loadProductCandidates}/
 * {@code loadCombinationCandidates}/{@code loadSleepObservation}/{@code loadWeatherObservation}이
 * 전부 "대상 없음"을 반환한다 — 그래서 지금은 분석을 실행하면 항상 HOLD(NO_TARGET)가 나온다. 각
 * 메서드의 Javadoc에 필요한 contract를 남겨뒀다 — 실제 연동이 들어오면 이 메서드들만 교체하면 된다.
 */
@Service
@RequiredArgsConstructor
public class EpisodeAnalysisService {

	private final EpisodeRepository episodeRepository;
	private final EpisodeAnalysisResultRepository episodeAnalysisResultRepository;
	private final CauseAnalysisEngine causeAnalysisEngine = new RuleBasedCauseAnalysisEngine();
	private final ResultCardAssembler resultCardAssembler = new ResultCardAssembler();

	@Transactional
	public ResultCardResult analyze(Long accountId, Long episodeId) {
		Episode episode = requireOwnedEpisode(accountId, episodeId);

		if (episode.getStatus() == EpisodeStatus.ANALYZED) {
			return loadExistingResult(episodeId, accountId);
		}
		if (episode.getStatus() != EpisodeStatus.INTAKE_COMPLETED) {
			throw new AfterglowException(ErrorCode.INVALID_EPISODE_STATE);
		}

		AnalysisInput input = buildAnalysisInput(accountId, episode);
		AnalysisResult analysisResult = causeAnalysisEngine.analyze(input);
		ResultCardResult resultCardResult = resultCardAssembler.assemble(analysisResult);

		episode.completeAnalysis();
		episodeAnalysisResultRepository.save(EpisodeAnalysisResult.from(episodeId, accountId, resultCardResult));

		return resultCardResult;
	}

	public ResultCardResult getResult(Long accountId, Long episodeId) {
		requireOwnedEpisode(accountId, episodeId);
		return loadExistingResult(episodeId, accountId);
	}

	private ResultCardResult loadExistingResult(Long episodeId, Long accountId) {
		return episodeAnalysisResultRepository.findByEpisodeIdAndAccountId(episodeId, accountId)
				.map(EpisodeAnalysisResult::toResultCardResult)
				.orElseThrow(() -> new NotFoundException("분석 결과를 찾을 수 없습니다."));
	}

	private AnalysisInput buildAnalysisInput(Long accountId, Episode episode) {
		LocalDate analysisDate = episode.getCreatedAt().toLocalDate();
		return new AnalysisInput(
				analysisDate,
				loadProductCandidates(accountId, episode, analysisDate),
				loadCombinationCandidates(accountId),
				loadSleepObservation(accountId, analysisDate),
				loadWeatherObservation(accountId, analysisDate)
		);
	}

	/**
	 * BLOCKED — Vanity에 ProductRepository가 아직 없어 계정의 실제 보유 제품/사용 시작일을 조회할 방법이
	 * 없다. 연동이 들어오면 이 메서드만 교체하면 된다. 필요한 contract:
	 * <ul>
	 *   <li>계정의 제품 목록과 각 제품의 실제 사용 시작일. 개봉 시기가 raw 값(예: 최근/1~3개월/6개월
	 *       이상)으로만 있다면 분석 기준일 기준 최근=-14일/1~3개월=-60일/6개월 이상=-180일로 환산하고,
	 *       추정값이면 근거 강도를 한 단계 낮춘다(강→중, 중→약). 개봉 시기 자체가 없으면 WEAK로 고정하고
	 *       제품 등록일부터 관찰한다 — 이 환산 로직은 이번 PR에서 아직 구현하지 않았다(실제 raw 필드
	 *       모양을 몰라서 추측하지 않음).</li>
	 *   <li>Routine에 의한 중단→재개는 이 기준일을 갱신하지 않는다({@link com.afterglow.domain.episode.routine.domain.RoutineProductLifecycle}
	 *       참고) — 이번 PR에서 Analysis 쪽 연결은 하지 않았다.</li>
	 *   <li>같은 기간(증상 시작 전후) 사용을 시작한 다른 제품 수.</li>
	 * </ul>
	 */
	private List<ProductCandidateInput> loadProductCandidates(Long accountId, Episode episode, LocalDate analysisDate) {
		return List.of();
	}

	/** BLOCKED — 같은 이유로 계정의 보유 제품 중 충돌 조합(InteractionTag 쌍)을 조회할 방법이 없다. */
	private List<CombinationCandidateInput> loadCombinationCandidates(Long accountId) {
		return List.of();
	}

	/**
	 * BLOCKED — Tracking에 엔티티/Repository가 아직 없어(dev 기준) 수면 관측 데이터 자체가 없다. 추가로
	 * CheckIn(IMPROVED/SAME/WORSE)을 sleep/weather match의 "증상 일치" source로 어떻게 매핑하는지 명세상
	 * 확정 근거가 부족하다 — 시점상으로도 CheckIn은 Routine 시작 이후에만 생기는데 최초 분석은 Routine
	 * 이전에 실행되므로, 최소한 "최초 분석"에는 CheckIn을 쓸 수 없다. 임의로 매핑을 만들지 않고 null(대상
	 * 없음)로 남긴다.
	 */
	private ObservationCandidateInput loadSleepObservation(Long accountId, LocalDate analysisDate) {
		return null;
	}

	/** BLOCKED — 위와 같은 이유로 날씨 관측 데이터도 없다. */
	private ObservationCandidateInput loadWeatherObservation(Long accountId, LocalDate analysisDate) {
		return null;
	}

	private Episode requireOwnedEpisode(Long accountId, Long episodeId) {
		return episodeRepository.findByIdAndAccountId(episodeId, accountId)
				.orElseThrow(() -> new NotFoundException("에피소드를 찾을 수 없습니다."));
	}
}
