package com.afterglow.domain.episode.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.afterglow.domain.episode.analysis.domain.CandidateType;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResult;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResultRepository;
import com.afterglow.domain.episode.checkin.domain.CheckIn;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.checkin.domain.Day3Verdict;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.intake.api.EpisodeMultiSummaryResponse;
import com.afterglow.domain.episode.intake.api.EpisodeMultiSummaryResponse.CauseCount;
import com.afterglow.domain.episode.intake.api.EpisodeMultiSummaryResponse.CombinationCausePattern;
import com.afterglow.domain.episode.intake.api.EpisodeMultiSummaryResponse.ProductCausePattern;
import com.afterglow.domain.episode.intake.api.EpisodeMultiSummaryResponse.ProductDetail;
import com.afterglow.domain.episode.routine.application.RoutineService;
import com.afterglow.domain.episode.routine.domain.Routine;
import com.afterglow.domain.episode.routine.domain.RoutineRepository;
import com.afterglow.domain.vanity.infrastructure.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Figma E3("3회차를 모아봤어요") 최소 조회 — 새 판정/추론 알고리즘을 만들지 않고, 이미 존재하는 Episode/
 * EpisodeAnalysisResult/Routine/CheckIn/Vanity Product 데이터를 단순 조회해 집계만 한다.
 *
 * <p><b>완료 Episode(2026-08-20 확정, 2026-08-21 정정)</b>: "Day3 verdict가 존재하는 Episode" —
 * {@link Day3Verdict#WITHHELD}의 기존 enum semantics(javadoc: "응답이 2일 이하라 판정하지 않음")를
 * 그대로 따르면 이건 "판정이 없는" 상태이지 verdict가 존재하는 상태가 아니다. 그래서 완료 판정은
 * Routine 존재 여부만이 아니라, 기존 {@code RoutineService#judgeDay3}가 반환한 verdict가 WITHHELD가
 * 아닌 경우로 좁힌다 — 새 판정 로직을 만든 게 아니라 이미 있는 enum의 뜻을 그대로 적용한 것이다.
 *
 * <p><b>결제(2026-08-20)</b>: Figma는 유료 CTA 뒤에 이 데이터를 두지만 Manyfast가 결제/구독을 범위
 * 제외했으므로, 이 서비스는 결제 여부를 전혀 확인하지 않고 데이터만 반환한다.
 */
@Service
@RequiredArgsConstructor
public class EpisodeMultiSummaryService {

	private static final int REQUIRED_COMPLETED_EPISODES = 3;

	private final EpisodeRepository episodeRepository;
	private final EpisodeAnalysisResultRepository episodeAnalysisResultRepository;
	private final RoutineRepository routineRepository;
	private final RoutineService routineService;
	private final CheckInRepository checkInRepository;
	private final ProductRepository productRepository;

	public EpisodeMultiSummaryResponse getMultiSummary(Long accountId) {
		List<EpisodeContext> completed = episodeRepository.findByAccountIdOrderByCreatedAtDescIdDesc(accountId).stream()
				.map(episode -> toContext(accountId, episode))
				.filter(context -> isCompleted(accountId, context))
				.toList();

		int completedCount = completed.size();
		if (completedCount < REQUIRED_COMPLETED_EPISODES) {
			return new EpisodeMultiSummaryResponse(false, completedCount, List.of(), null, List.of(), List.of());
		}

		List<EpisodeContext> target = completed.subList(0, REQUIRED_COMPLETED_EPISODES);

		return new EpisodeMultiSummaryResponse(
				true,
				completedCount,
				causeCounts(target),
				averageImprovementDay(target),
				productPatterns(accountId, target),
				combinationPatterns(target)
		);
	}

	private EpisodeContext toContext(Long accountId, Episode episode) {
		Routine routine = routineRepository.findByEpisodeIdAndAccountId(episode.getId(), accountId).orElse(null);
		EpisodeAnalysisResult analysis = episodeAnalysisResultRepository
				.findByEpisodeIdAndAccountId(episode.getId(), accountId)
				.orElse(null);
		return new EpisodeContext(episode, routine, analysis);
	}

	/** Routine이 없으면 애초에 verdict를 구할 수 없어 미완료. Routine이 있으면 기존 judgeDay3를 그대로 호출해 WITHHELD(판정 없음)만 제외한다. */
	private boolean isCompleted(Long accountId, EpisodeContext context) {
		if (context.routine() == null) {
			return false;
		}
		Day3Verdict verdict = routineService.judgeDay3(accountId, context.episode().getId()).verdict();
		return verdict != Day3Verdict.WITHHELD;
	}

	/** "대표 원인" 하나로 줄이지 않고, causeType별 등장 횟수를 전부 그대로 반환한다(동률/최소 횟수 정책 없음). */
	private List<CauseCount> causeCounts(List<EpisodeContext> target) {
		Map<CandidateType, Long> counts = target.stream()
				.map(EpisodeContext::analysis)
				.filter(Objects::nonNull)
				.map(EpisodeAnalysisResult::getCauseType)
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		return counts.entrySet().stream()
				.map(entry -> new CauseCount(entry.getKey(), entry.getValue().intValue()))
				.toList();
	}

	/** 존재하는 첫 IMPROVED dayNumber만 평균한다 — 하나도 없으면 null(0으로 채우지 않는다). */
	private Double averageImprovementDay(List<EpisodeContext> target) {
		List<Integer> days = target.stream()
				.map(this::firstImprovedDay)
				.filter(Objects::nonNull)
				.toList();

		if (days.isEmpty()) {
			return null;
		}
		return days.stream().mapToInt(Integer::intValue).average().orElseThrow();
	}

	/** Day1→Day3 순서로 훑어 처음 IMPROVED가 나온 날을 찾는다. Day4 이상은 스키마에 없어 만들지 않는다. */
	private Integer firstImprovedDay(EpisodeContext context) {
		Routine routine = context.routine();
		for (int day = 1; day <= Routine.DURATION_DAYS; day++) {
			CheckInStatus status = checkInRepository
					.findByEpisodeIdAndCheckInDate(context.episode().getId(), routine.dateOf(day))
					.map(CheckIn::getStatus)
					.orElse(null);
			if (status == CheckInStatus.IMPROVED) {
				return day;
			}
		}
		return null;
	}

	/** episode마다의 PRODUCT cause를 그룹/집계 없이 원본 그대로 반환한다 — "반복 패턴" 정의는 새 정책이 필요해 만들지 않았다. */
	private List<ProductCausePattern> productPatterns(Long accountId, List<EpisodeContext> target) {
		List<ProductCausePattern> patterns = new ArrayList<>();
		for (EpisodeContext context : target) {
			EpisodeAnalysisResult analysis = context.analysis();
			if (analysis == null || analysis.getCauseType() != CandidateType.PRODUCT) {
				continue;
			}
			Long productId = analysis.getEvidenceProductId();
			ProductDetail detail = productRepository.findByIdAndAccountId(productId, accountId)
					.map(product -> new ProductDetail(product.getType(), product.getInteractionTags()))
					.orElse(null);
			patterns.add(new ProductCausePattern(context.episode().getId(), productId, detail));
		}
		return patterns;
	}

	private List<CombinationCausePattern> combinationPatterns(List<EpisodeContext> target) {
		List<CombinationCausePattern> patterns = new ArrayList<>();
		for (EpisodeContext context : target) {
			EpisodeAnalysisResult analysis = context.analysis();
			if (analysis == null || analysis.getCauseType() != CandidateType.COMBINATION) {
				continue;
			}
			patterns.add(new CombinationCausePattern(
					context.episode().getId(), analysis.getEvidenceTagA(), analysis.getEvidenceTagB()));
		}
		return patterns;
	}

	private record EpisodeContext(Episode episode, Routine routine, EpisodeAnalysisResult analysis) {
	}
}
