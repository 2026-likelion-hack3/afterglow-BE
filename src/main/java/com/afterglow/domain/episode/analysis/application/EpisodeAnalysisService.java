package com.afterglow.domain.episode.analysis.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.analysis.domain.AnalysisInput;
import com.afterglow.domain.episode.analysis.domain.AnalysisResult;
import com.afterglow.domain.episode.analysis.domain.CauseAnalysisEngine;
import com.afterglow.domain.episode.analysis.domain.CombinationCandidateInput;
import com.afterglow.domain.episode.analysis.domain.ConflictPlacement;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResult;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResultRepository;
import com.afterglow.domain.episode.analysis.domain.ObservationCandidateInput;
import com.afterglow.domain.episode.analysis.domain.ProductCandidateInput;
import com.afterglow.domain.episode.analysis.domain.ProductObservationWindow;
import com.afterglow.domain.episode.analysis.domain.RecordCoverage;
import com.afterglow.domain.episode.analysis.domain.RuleBasedCauseAnalysisEngine;
import com.afterglow.domain.episode.card.domain.ResultCardAssembler;
import com.afterglow.domain.episode.card.domain.ResultCardResult;
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
 * <p><b>symptomStartDate 미확정 — 후속 기획 확정 지점(2026-08-20, Known limitation)</b>:
 * {@code Intake.onsetPeriod}(증상 시작 시기 4구간: TODAY/2~3일 전/1주 전/2주 이상)를 실제 날짜로
 * 환산하는 규칙이 Manyfast/코드 어디에도 없다(OpeningPeriod와 달리 이번 결정에도 포함되지 않음) — 새
 * 환산 규칙을 임의로 발명하지 않는다. 그래서 {@code symptomStartDate}는 항상 {@code analysisDate}
 * (episode 생성일)를 쓰지만, {@code onsetPeriod != TODAY}일 때는 이 값이 실제 증상 시작일보다 늦다
 * (analysisDate ≥ 실제 증상 시작일이 항상 성립하므로). 이 오차를 그대로 두면 "실제로는 증상이 이미 시작된
 * 뒤에 쓰기 시작한 제품"이 "증상 전에 시작"한 것처럼 잘못 판정될 위험(false positive, STRONG/MEDIUM 오생성)이
 * 있다 — 그래서 {@code onsetPeriod == TODAY}일 때만 {@code symptomStartDateReliable=true}로 넘기고,
 * 그 외에는 false로 넘겨 {@link RuleBasedCauseAnalysisEngine}이 timing 계산 없이 WEAK로 고정하게 한다
 * (가장 보수적인 제한 — {@link ProductCandidateInput#symptomStartDateReliable} 참고). onsetPeriod → 날짜
 * 환산 규칙이 기획에서 확정되면 이 보수적 제한을 없애고 정확한 {@code symptomStartDate}를 계산하면 된다.
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
		LocalDate symptomStartDate = analysisDate;
		boolean symptomStartDateReliable = episode.getIntake().getOnsetPeriod() == OnsetPeriod.TODAY;

		List<VanityQueryResponse> products = vanityQueryService.findProductsByAccountId(accountId);
		List<VanityProductReference> references = products.stream()
				.map(product -> VanityProductReference.of(product, analysisDate))
				.toList();
		List<Routine> routines = routineRepository.findByAccountIdOrderByStartDateAscIdAsc(accountId);
		Set<LocalDate> checkInDates = findCheckInDates(accountId, references, analysisDate);

		return new AnalysisInput(
				analysisDate,
				loadProductCandidates(references, routines, checkInDates, analysisDate, symptomStartDate, symptomStartDateReliable),
				loadCombinationCandidates(references, routines, checkInDates, analysisDate),
				loadSleepObservation(accountId, analysisDate),
				loadWeatherObservation(accountId, analysisDate));
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
			LocalDate analysisDate, LocalDate symptomStartDate, boolean symptomStartDateReliable) {
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
							symptomStartDateReliable,
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
	 * BLOCKED(threshold만) — Tracking raw 15일 조회와 CheckIn 정렬({@link WeatherObservationDay#align})은
	 * 실제로 연결했다. temperature/minTemperature/humidity/uvIndex 중 무엇을, 어떤 값으로 "일치"로 볼지
	 * threshold가 기획에 아직 없어 matchedObservationCount/candidate 생성은 하지 않는다 — 임의로 추측해서
	 * 만들지 않는다(사용자 지시). threshold가 오면 이 메서드 안에서 {@code alignedDays}를 이용해 candidate를
	 * 만들면 된다.
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
