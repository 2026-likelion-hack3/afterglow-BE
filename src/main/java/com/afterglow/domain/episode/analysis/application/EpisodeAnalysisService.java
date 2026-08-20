package com.afterglow.domain.episode.analysis.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.analysis.domain.AnalysisInput;
import com.afterglow.domain.episode.analysis.domain.AnalysisResult;
import com.afterglow.domain.episode.analysis.domain.CandidateType;
import com.afterglow.domain.episode.analysis.domain.CauseAnalysisEngine;
import com.afterglow.domain.episode.analysis.domain.CombinationCandidateInput;
import com.afterglow.domain.episode.analysis.domain.CombinationEvidence;
import com.afterglow.domain.episode.analysis.domain.ConflictPlacement;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResult;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResultRepository;
import com.afterglow.domain.episode.analysis.domain.ObservationCandidateInput;
import com.afterglow.domain.episode.analysis.domain.ProductCandidateInput;
import com.afterglow.domain.episode.analysis.domain.ProductObservationWindow;
import com.afterglow.domain.episode.analysis.domain.RecordCoverage;
import com.afterglow.domain.episode.analysis.domain.RuleBasedCauseAnalysisEngine;
import com.afterglow.domain.episode.analysis.domain.TimingEvidence;
import com.afterglow.domain.episode.card.domain.ResultCard;
import com.afterglow.domain.episode.card.domain.ResultCardAssembler;
import com.afterglow.domain.episode.card.domain.ResultCardResult;
import com.afterglow.domain.episode.card.domain.ResultCardType;
import com.afterglow.domain.episode.checkin.application.CheckInService;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.EpisodeStatus;
import com.afterglow.domain.episode.domain.OnsetPeriod;
import com.afterglow.domain.episode.routine.domain.ProductUsageEvent;
import com.afterglow.domain.episode.routine.domain.Routine;
import com.afterglow.domain.episode.routine.domain.RoutineItemUsage;
import com.afterglow.domain.episode.routine.domain.RoutineProductLifecycle;
import com.afterglow.domain.episode.routine.domain.RoutineRepository;
import com.afterglow.domain.tracking.daily.application.TrackingQueryResponse;
import com.afterglow.domain.tracking.daily.application.TrackingQueryService;
import com.afterglow.domain.tracking.daily.domain.SleepLevel;
import com.afterglow.domain.vanity.CombinationRule;
import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.UsageTiming;
import com.afterglow.domain.vanity.application.VanityQueryResponse;
import com.afterglow.domain.vanity.application.VanityQueryService;
import com.afterglow.domain.vanity.infrastructure.CombinationRuleRepository;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.afterglow.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * 2.4 통합 분석 orchestration — account ownership 확인 → Episode 상태 확인 → raw data 조회 →
 * {@link RuleBasedCauseAnalysisEngine} 실행 → {@link ResultCardAssembler}로 카드 조립 → 저장 → 반환.
 * 이미 {@code ANALYZED}면 재실행하지 않고 저장된 결과를 그대로 돌려준다(idempotent, 기존 설계 그대로).
 *
 * <p><b>Vanity/Tracking/CheckIn 실제 연동(2026-08-20)</b>: {@code loadProductCandidates}/
 * {@code loadCombinationCandidates}/{@code loadSleepObservation}은 {@link VanityQueryService}·
 * {@link CombinationRuleRepository}·{@link TrackingQueryService}·{@link CheckInService}·
 * {@link RoutineProductLifecycle}로 실제 데이터를 채운다. {@code loadWeatherObservation}만 여전히
 * "대상 없음"을 반환한다 — Tracking raw 조회/CheckIn 정렬({@link WeatherObservationDay#align})까지는
 * 실제로 연결했지만, temperature/minTemperature/humidity/uvIndex 중 무엇을, 어떤 값으로 "일치"로 볼지
 * threshold가 기획에 아직 없어서다(docs/domains/episode.md Pending Decisions). 기획 답변이 오면
 * {@code loadWeatherObservation}만 교체하면 된다 — 나머지 파이프라인은 이미 준비돼 있다.
 *
 * <p><b>symptomStartDate — onsetPeriod → 날짜 환산 확정(2026-08-20, 기획 확정)</b>:
 * {@code Intake.onsetPeriod}(증상 시작 시기 4구간: TODAY/2~3일 전/1주 전/2주 이상)를 실제 날짜로
 * 환산하는 규칙이 확정됐다 — TODAY=분석 기준일, 2~3일 전=3일 전, 1주 전=7일 전, 2주 이상=14일 전
 * ({@link #symptomStartDateOf}). 이전에는 이 환산 규칙이 없어 TODAY가 아니면 {@code symptomStartDate}를
 * 신뢰할 수 없는 값으로 보고 {@link RuleBasedCauseAnalysisEngine}이 timing 계산 없이 WEAK로 고정하는
 * 보수적 제한({@code symptomStartDateReliable})이 있었으나, 규칙이 확정되며 그 플래그를 제거했다 — 이제
 * 모든 onsetPeriod 값에서 실제 날짜를 계산해 기존 Product timing 강도 계산에 그대로 사용한다.
 */
@Service
@RequiredArgsConstructor
public class EpisodeAnalysisService {

	/** 제품 후보 timing 창(1~14일 전) — {@code RuleBasedCauseAnalysisEngine.PRODUCT_TIMING_WINDOW_DAYS}와 동일한 값(엔진이 private라 여기서도 상수로 둔다). */
	private static final long PRODUCT_TIMING_WINDOW_DAYS = 14;

	/** Sleep observation 기본 관측 범위(2026-08-20 확정) — 분석 기준일 포함 14일. */
	private static final long SLEEP_OBSERVATION_WINDOW_DAYS = 14;

	/** Weather는 전날 대비 비교가 필요할 수 있어 하루 더 넉넉히(15일) 가져온다 — threshold 미확정이라 아직 판정에는 안 쓴다. */
	private static final long WEATHER_OBSERVATION_WINDOW_DAYS = 15;

	private final EpisodeRepository episodeRepository;
	private final EpisodeAnalysisResultRepository episodeAnalysisResultRepository;
	private final VanityQueryService vanityQueryService;
	private final CombinationRuleRepository combinationRuleRepository;
	private final TrackingQueryService trackingQueryService;
	private final CheckInService checkInService;
	private final RoutineRepository routineRepository;
	private final CauseAnalysisEngine causeAnalysisEngine = new RuleBasedCauseAnalysisEngine();
	private final ResultCardAssembler resultCardAssembler = new ResultCardAssembler();
	private final RoutineProductLifecycle routineProductLifecycle = new RoutineProductLifecycle();

	@Transactional
	public ResultCardResult analyze(Long accountId, Long episodeId) {
		Episode episode = requireOwnedEpisode(accountId, episodeId);

		if (episode.getStatus() == EpisodeStatus.ANALYZED) {
			return withContinueUseProducts(loadExistingResult(episodeId, accountId), accountId);
		}
		if (episode.getStatus() != EpisodeStatus.INTAKE_COMPLETED) {
			throw new AfterglowException(ErrorCode.INVALID_EPISODE_STATE);
		}

		AnalysisInput input = buildAnalysisInput(accountId, episode);
		AnalysisResult analysisResult = causeAnalysisEngine.analyze(input);
		ResultCardResult resultCardResult = resultCardAssembler.assemble(analysisResult);

		episode.completeAnalysis();
		episodeAnalysisResultRepository.save(EpisodeAnalysisResult.from(episodeId, accountId, resultCardResult));

		return withContinueUseProducts(resultCardResult, accountId);
	}

	public ResultCardResult getResult(Long accountId, Long episodeId) {
		requireOwnedEpisode(accountId, episodeId);
		return withContinueUseProducts(loadExistingResult(episodeId, accountId), accountId);
	}

	/**
	 * CONTINUE_USE("오늘 사용할 것") 카드에 실제 보유 제품 목록을 채운다(2026-08-20 RC1 hotfix).
	 *
	 * <p><b>RC1 CONTINUE_USE 정책(2026-08-20 제품 결정 — 기존 코드에서 발견한 규칙이 아니라 이번에
	 * 새로 확정한 최소 deterministic 규칙, AI 추론 사용 안 함)</b>: Routine/Analysis 어디에도 "어떤
	 * 보유 제품을 계속 써도 되는지" 판단하는 기존 로직이 없었다(조사 결과, Routine은 사용자가 고른 값을
	 * 그대로 저장할 뿐 positive selection을 하지 않는다) — 그래서 "원인으로 지목되지 않은 나머지 전체"를
	 * 보여주는 이전 구현(2026-08-20 초안)은 근거 없는 추천이었다. 대신 아래 좁은 규칙만 적용한다:
	 * <ol>
	 *   <li>Analysis가 HOLD면(사유 무관) 항상 빈 배열.</li>
	 *   <li>HOLD가 아니어도 top 원인이 PRODUCT/COMBINATION이 아니면(SLEEP/WEATHER) 항상 빈 배열 —
	 *       제품이 원인이 아니라는 것이 "이 제품들을 써도 된다"를 의미하지 않는다.</li>
	 *   <li>그 외의 경우, 계정 Vanity 보유 제품 중 {@link InteractionTag#LOW_IRRITATION}이 명시적으로
	 *       있고 RETINOL/ACID/VITAMIN_C/HIGH_CONCENTRATION이 전혀 없으며, 이번 분석의 원인 제품(들)이
	 *       아니고, Routine 이력상 현재 중단 상태도 아닌 제품만 포함한다. 여러 개면 전부 반환하고(임의로
	 *       하나를 고르지 않는다), 없으면 빈 배열이다 — 빈 배열은 오류가 아니다.</li>
	 * </ol>
	 * LOW_IRRITATION은 Product에 이미 명시적으로 저장된 태그만 신뢰한다 — OpenAI/OCR로 새로 추론하지
	 * 않는다({@code InteractionTagMatcher}도 LOW_IRRITATION/HIGH_CONCENTRATION은 매칭 대상에서 제외돼
	 * 있어, 이 태그는 사용자가 직접 입력했거나 애초에 없는 값이다).
	 *
	 * <p>{@link ResultCardAssembler}/{@link EpisodeAnalysisResult}는 건드리지 않고 응답 조립 시점에만
	 * 후처리로 붙인다(영속화 안 함). deterministic Analysis 결과(hold/topCandidate/confidence/첫 번째
	 * 카드) 자체는 전혀 바꾸지 않는다.
	 */
	private ResultCardResult withContinueUseProducts(ResultCardResult result, Long accountId) {
		ResultCard firstCard = result.cards().get(0);
		boolean eligibleForContinueUse = !result.hold()
				&& (firstCard.causeType() == CandidateType.PRODUCT || firstCard.causeType() == CandidateType.COMBINATION);
		if (!eligibleForContinueUse) {
			return result;
		}

		List<VanityQueryResponse> products = vanityQueryService.findProductsByAccountId(accountId);
		if (products.isEmpty()) {
			return result;
		}

		Set<Long> causeProductIds = causeProductIds(firstCard, products);
		List<Routine> routines = routineRepository.findByAccountIdOrderByStartDateAscIdAsc(accountId);

		List<Long> continueUseProductIds = products.stream()
				.filter(EpisodeAnalysisService::isLowIrritationEligible)
				.filter(product -> !causeProductIds.contains(product.productId()))
				.filter(product -> !isCurrentlyStopped(routines, product.productId()))
				.map(VanityQueryResponse::productId)
				.toList();

		List<ResultCard> updatedCards = result.cards().stream()
				.map(card -> card.type() == ResultCardType.CONTINUE_USE
						? new ResultCard(card.type(), card.causeType(), card.evidence(), card.coverageDays(), continueUseProductIds)
						: card)
				.toList();

		return ResultCardResult.determined(result.confidence(), updatedCards);
	}

	/** LOW_IRRITATION이 명시적으로 있고, RETINOL/ACID/VITAMIN_C/HIGH_CONCENTRATION은 전혀 없어야 한다(RC1 정책 4·5번). */
	private static boolean isLowIrritationEligible(VanityQueryResponse product) {
		Set<InteractionTag> tags = product.interactionTags();
		if (!tags.contains(InteractionTag.LOW_IRRITATION)) {
			return false;
		}
		return !tags.contains(InteractionTag.RETINOL)
				&& !tags.contains(InteractionTag.ACID)
				&& !tags.contains(InteractionTag.VITAMIN_C)
				&& !tags.contains(InteractionTag.HIGH_CONCENTRATION);
	}

	private Set<Long> causeProductIds(ResultCard firstCard, List<VanityQueryResponse> products) {
		if (firstCard.evidence() instanceof TimingEvidence timing) {
			return Set.of(timing.productId());
		}
		if (firstCard.evidence() instanceof CombinationEvidence combo) {
			return combinationProductIds(combo, products);
		}
		return Set.of();
	}

	/** 계정 Routine 이력상 이 제품의 가장 최근 사건이 DISCONTINUE면(재개로 이어지지 않았으면) 중단 상태로 본다. */
	private boolean isCurrentlyStopped(List<Routine> routines, Long productId) {
		List<ProductUsageEvent> events = routineProductLifecycle.historyOf(routines, productId);
		if (events.isEmpty()) {
			return false;
		}
		return events.get(events.size() - 1).usage() == RoutineItemUsage.DISCONTINUE;
	}

	/** CombinationEvidence의 태그 쌍을 실제로 가진 계정 제품 쌍을 찾는다 — {@link #collectCombinationCandidate}와 동일한 매칭 규칙. */
	private Set<Long> combinationProductIds(CombinationEvidence combo, List<VanityQueryResponse> products) {
		InteractionTag tagA = InteractionTag.valueOf(combo.tagA());
		InteractionTag tagB = InteractionTag.valueOf(combo.tagB());
		Set<Long> matched = new HashSet<>();
		for (int i = 0; i < products.size(); i++) {
			for (int j = i + 1; j < products.size(); j++) {
				VanityQueryResponse first = products.get(i);
				VanityQueryResponse second = products.get(j);
				boolean firstHasA = first.interactionTags().contains(tagA);
				boolean firstHasB = first.interactionTags().contains(tagB);
				boolean secondHasA = second.interactionTags().contains(tagA);
				boolean secondHasB = second.interactionTags().contains(tagB);
				if ((firstHasA && secondHasB) || (firstHasB && secondHasA)) {
					matched.add(first.productId());
					matched.add(second.productId());
				}
			}
		}
		return matched;
	}

	private ResultCardResult loadExistingResult(Long episodeId, Long accountId) {
		return episodeAnalysisResultRepository.findByEpisodeIdAndAccountId(episodeId, accountId)
				.map(EpisodeAnalysisResult::toResultCardResult)
				.orElseThrow(() -> new NotFoundException("분석 결과를 찾을 수 없습니다."));
	}

	private AnalysisInput buildAnalysisInput(Long accountId, Episode episode) {
		LocalDate analysisDate = episode.getCreatedAt().toLocalDate();
		LocalDate symptomStartDate = symptomStartDateOf(episode.getIntake().getOnsetPeriod(), analysisDate);

		List<VanityQueryResponse> products = vanityQueryService.findProductsByAccountId(accountId);
		List<VanityProductReference> references = products.stream()
				.map(product -> VanityProductReference.of(product, analysisDate))
				.toList();
		List<Routine> routines = routineRepository.findByAccountIdOrderByStartDateAscIdAsc(accountId);
		Set<LocalDate> checkInDates = findCheckInDates(accountId, references, analysisDate);

		return new AnalysisInput(
				analysisDate,
				loadProductCandidates(references, routines, checkInDates, analysisDate, symptomStartDate),
				loadCombinationCandidates(references, routines, checkInDates, analysisDate),
				loadSleepObservation(accountId, analysisDate),
				loadWeatherObservation(accountId, analysisDate));
	}

	/**
	 * onsetPeriod → 실제 증상 시작일 환산(2026-08-20 기획 확정): 오늘=분석 기준일, 2~3일 전=3일 전,
	 * 1주 전=7일 전, 2주 이상=14일 전. 새 기간 추정 규칙(예: "2주 이상"을 14일보다 더 먼 날짜로 보는 것)은
	 * 추가하지 않는다 — 확정된 값 그대로만 쓴다.
	 */
	private static LocalDate symptomStartDateOf(OnsetPeriod onsetPeriod, LocalDate analysisDate) {
		return switch (onsetPeriod) {
			case TODAY -> analysisDate;
			case TWO_TO_THREE_DAYS_AGO -> analysisDate.minusDays(3);
			case ONE_WEEK_AGO -> analysisDate.minusDays(7);
			case TWO_WEEKS_OR_MORE -> analysisDate.minusDays(14);
		};
	}

	/** 모든 후보의 관측 window가 들어올 수 있는 가장 이른 날짜(가장 오래된 referenceDate)부터 분석 기준일까지 한 번에 조회한다. */
	private Set<LocalDate> findCheckInDates(Long accountId, List<VanityProductReference> references, LocalDate analysisDate) {
		if (references.isEmpty()) {
			return Set.of();
		}
		LocalDate earliestReference = references.stream()
				.map(VanityProductReference::referenceDate)
				.min(Comparator.naturalOrder())
				.orElse(analysisDate);
		return checkInService.findCheckInDatesByAccountAndPeriod(accountId, earliestReference, analysisDate);
	}

	private List<ProductCandidateInput> loadProductCandidates(
			List<VanityProductReference> references, List<Routine> routines, Set<LocalDate> checkInDates,
			LocalDate analysisDate, LocalDate symptomStartDate) {
		if (references.isEmpty()) {
			return List.of();
		}

		LocalDate changeWindowStart = symptomStartDate.minusDays(PRODUCT_TIMING_WINDOW_DAYS);
		long changedProductCount = references.stream()
				.map(VanityProductReference::referenceDate)
				.filter(date -> !date.isBefore(changeWindowStart) && date.isBefore(symptomStartDate))
				.count();

		return references.stream()
				.map(reference -> {
					LocalDate discontinuedAt = firstDiscontinuationOnOrAfter(
							routines, reference.productId(), reference.referenceDate());
					ProductObservationWindow window = ProductObservationWindow.of(
							reference.referenceDate(), analysisDate, discontinuedAt);
					return new ProductCandidateInput(
							reference.productId(),
							reference.referenceDate(),
							symptomStartDate,
							(int) changedProductCount,
							reference.certainty(),
							window.coverageDays(checkInDates));
				})
				.toList();
	}

	private List<CombinationCandidateInput> loadCombinationCandidates(
			List<VanityProductReference> references, List<Routine> routines, Set<LocalDate> checkInDates, LocalDate analysisDate) {
		if (references.size() < 2) {
			return List.of();
		}
		List<CombinationRule> rules = combinationRuleRepository.findAll().stream()
				.filter(rule -> !rule.isSameTagThresholdRule())
				.toList();
		if (rules.isEmpty()) {
			return List.of();
		}

		List<CombinationCandidateInput> combinations = new ArrayList<>();
		for (int i = 0; i < references.size(); i++) {
			for (int j = i + 1; j < references.size(); j++) {
				VanityProductReference first = references.get(i);
				VanityProductReference second = references.get(j);
				for (CombinationRule rule : rules) {
					collectCombinationCandidate(first, second, rule, routines, checkInDates, analysisDate)
							.ifPresent(combinations::add);
				}
			}
		}
		return combinations;
	}

	private Optional<CombinationCandidateInput> collectCombinationCandidate(
			VanityProductReference first, VanityProductReference second, CombinationRule rule,
			List<Routine> routines, Set<LocalDate> checkInDates, LocalDate analysisDate) {
		boolean firstHasA = first.interactionTags().contains(rule.getTagA());
		boolean firstHasB = first.interactionTags().contains(rule.getTagB());
		boolean secondHasA = second.interactionTags().contains(rule.getTagA());
		boolean secondHasB = second.interactionTags().contains(rule.getTagB());
		if (!((firstHasA && secondHasB) || (firstHasB && secondHasA))) {
			return Optional.empty();
		}

		ConflictPlacement placement = placementOf(first.usageTiming(), second.usageTiming());
		LocalDate referenceDate = combinationReferenceDate(first, second, placement);

		LocalDate firstDiscontinuedAt = firstDiscontinuationOnOrAfter(routines, first.productId(), referenceDate);
		LocalDate secondDiscontinuedAt = firstDiscontinuationOnOrAfter(routines, second.productId(), referenceDate);
		LocalDate discontinuedAt = earliestNonNull(firstDiscontinuedAt, secondDiscontinuedAt);

		ProductObservationWindow window = ProductObservationWindow.of(referenceDate, analysisDate, discontinuedAt);
		return Optional.of(new CombinationCandidateInput(
				rule.getTagA().name(), rule.getTagB().name(), placement, window.coverageDays(checkInDates)));
	}

	/** 같은 time slot(아침 또는 저녁)을 공유하면 SAME_TIME_SLOT, 아침/저녁으로 정확히 갈리면 SPLIT_AM_PM, 그 외(usageTiming 미입력 등)는 NONE. */
	private ConflictPlacement placementOf(UsageTiming first, UsageTiming second) {
		boolean sharedMorning = isUsedAt(first, UsageTiming.MORNING) && isUsedAt(second, UsageTiming.MORNING);
		boolean sharedEvening = isUsedAt(first, UsageTiming.EVENING) && isUsedAt(second, UsageTiming.EVENING);
		if (sharedMorning || sharedEvening) {
			return ConflictPlacement.SAME_TIME_SLOT;
		}
		boolean split = (isUsedAt(first, UsageTiming.MORNING) && isUsedAt(second, UsageTiming.EVENING))
				|| (isUsedAt(first, UsageTiming.EVENING) && isUsedAt(second, UsageTiming.MORNING));
		return split ? ConflictPlacement.SPLIT_AM_PM : ConflictPlacement.NONE;
	}

	private boolean isUsedAt(UsageTiming productTiming, UsageTiming targetTiming) {
		if (productTiming == null) {
			return false;
		}
		return productTiming == UsageTiming.BOTH || productTiming == targetTiming;
	}

	/**
	 * 두 제품 referenceDate 중 더 늦은 날짜가 기본이다. SAME_TIME_SLOT이 usageTiming 변경으로 처음
	 * 발생했다면(변경일이 그 기본값보다 늦다면) 변경일을 대신 쓴다(2026-08-20 확정).
	 */
	private LocalDate combinationReferenceDate(VanityProductReference first, VanityProductReference second, ConflictPlacement placement) {
		LocalDate reference = first.referenceDate().isAfter(second.referenceDate())
				? first.referenceDate() : second.referenceDate();
		if (placement != ConflictPlacement.SAME_TIME_SLOT) {
			return reference;
		}
		if (first.usageTimingChangedAt() != null && first.usageTimingChangedAt().isAfter(reference)) {
			reference = first.usageTimingChangedAt();
		}
		if (second.usageTimingChangedAt() != null && second.usageTimingChangedAt().isAfter(reference)) {
			reference = second.usageTimingChangedAt();
		}
		return reference;
	}

	private LocalDate firstDiscontinuationOnOrAfter(List<Routine> routines, Long productId, LocalDate referenceDate) {
		List<ProductUsageEvent> events = routineProductLifecycle.historyOf(routines, productId);
		return events.stream()
				.filter(event -> event.usage() == RoutineItemUsage.DISCONTINUE)
				.map(ProductUsageEvent::date)
				.filter(date -> !date.isBefore(referenceDate))
				.min(Comparator.naturalOrder())
				.orElse(null);
	}

	private LocalDate earliestNonNull(LocalDate a, LocalDate b) {
		if (a == null) {
			return b;
		}
		if (b == null) {
			return a;
		}
		return a.isBefore(b) ? a : b;
	}

	/**
	 * WELL/POOR + IMPROVED/SAME/WORSE 매칭({@link SleepMatchRule}, 2026-08-20 확정)을 실제 Tracking/CheckIn
	 * 데이터에 적용한다. NORMAL인 날, 또는 CheckIn이 없는 날은 관측 대상에서 제외한다(관측 자체가 없으므로
	 * observationCount에도 포함하지 않는다) — "일치 여부를 판단할 수 없는 날"을 분모에 넣지 않기 위함.
	 *
	 * <p><b>coverage(2026-08-20 정정)</b>: calendar-day span이 아니라 "실제 분석 가능한 observation이
	 * 존재하는 날짜 수" — 즉 이 메서드가 세는 {@code observationCount} 그 자체다(예: 8/1·8/7 두 날짜만
	 * 관측됐으면 calendar span은 7이어도 coverage는 2). 7일 게이트 임계값은 바뀌지 않았다 — 비교 대상만
	 * calendar span에서 record 건수로 바뀌었다({@link RecordCoverage} 참고).
	 */
	private ObservationCandidateInput loadSleepObservation(Long accountId, LocalDate analysisDate) {
		LocalDate from = analysisDate.minusDays(SLEEP_OBSERVATION_WINDOW_DAYS - 1);
		List<TrackingQueryResponse> tracking = trackingQueryService.findByPeriod(accountId, from, analysisDate);
		Map<LocalDate, CheckInStatus> checkInStatuses = checkInService.findCheckInStatusesByAccountAndPeriod(accountId, from, analysisDate);

		int observationCount = 0;
		int matchedObservationCount = 0;

		for (TrackingQueryResponse day : tracking) {
			SleepLevel sleepLevel = day.sleepLevel();
			if (sleepLevel == null || !SleepMatchRule.isObservable(sleepLevel)) {
				continue;
			}
			CheckInStatus status = checkInStatuses.get(day.recordedDate());
			if (status == null) {
				continue;
			}
			observationCount++;
			if (SleepMatchRule.matches(sleepLevel, status)) {
				matchedObservationCount++;
			}
		}

		if (observationCount == 0) {
			return null;
		}
		return new ObservationCandidateInput(observationCount, matchedObservationCount, new RecordCoverage(observationCount));
	}

	/**
	 * BLOCKED(threshold만, 2026-08-20 재확인 — 여전히 미확정) — Tracking raw 15일 조회와 CheckIn 정렬
	 * ({@link WeatherObservationDay#align})은 실제로 연결했다. "일치" 판정 규칙 자체는 확정되어
	 * {@link WeatherMatchRule}로 구현했지만(조건 충족+WORSE/SAME→일치, 조건 충족+IMPROVED→불일치, 조건
	 * 미충족+IMPROVED→일치, 조건 미충족+SAME/WORSE→불일치), 그 입력인 conditionMet — temperature/
	 * minTemperature/humidity/uvIndex 중 무엇을, 어떤 값으로 "날씨 조건 충족"으로 볼지 — 의 threshold가
	 * Manyfast에 여전히 없어 matchedObservationCount/candidate 생성은 하지 않는다 — 임의로 추측해서 만들지
	 * 않는다(사용자 지시). threshold가 오면 이 메서드 안에서 {@code alignedDays} 각 날짜의 conditionMet을
	 * 계산하고 {@link WeatherMatchRule#matches}에 넘겨 candidate를 만들면 된다 — 나머지 파이프라인(강도/
	 * ranking/confidence/HOLD)은 이미 {@link RuleBasedCauseAnalysisEngine}이 공통으로 처리한다.
	 */
	private ObservationCandidateInput loadWeatherObservation(Long accountId, LocalDate analysisDate) {
		LocalDate from = analysisDate.minusDays(WEATHER_OBSERVATION_WINDOW_DAYS - 1);
		List<TrackingQueryResponse> weatherRaw = trackingQueryService.findByPeriod(accountId, from, analysisDate);
		Map<LocalDate, CheckInStatus> checkInStatuses = checkInService.findCheckInStatusesByAccountAndPeriod(accountId, from, analysisDate);
		List<WeatherObservationDay> alignedDays = WeatherObservationDay.align(weatherRaw, checkInStatuses);
		// threshold 확정 전까지는 alignedDays를 판정에 쓰지 않는다 — 계산만 준비해 두고 candidate는 만들지 않는다.
		return null;
	}

	private Episode requireOwnedEpisode(Long accountId, Long episodeId) {
		return episodeRepository.findByIdAndAccountId(episodeId, accountId)
				.orElseThrow(() -> new NotFoundException("에피소드를 찾을 수 없습니다."));
	}
}
