package com.afterglow.domain.episode.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.episode.analysis.domain.FrequencyEvidence;
import com.afterglow.domain.episode.analysis.domain.TimingEvidence;
import com.afterglow.domain.episode.card.domain.ResultCardHoldReason;
import com.afterglow.domain.episode.card.domain.ResultCardResult;
import com.afterglow.domain.episode.card.domain.ResultCardType;
import com.afterglow.domain.episode.checkin.domain.CheckIn;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.EpisodeStatus;
import com.afterglow.domain.episode.domain.Intake;
import com.afterglow.domain.episode.domain.OnsetPeriod;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.domain.Symptom;
import com.afterglow.domain.episode.routine.domain.Routine;
import com.afterglow.domain.episode.routine.domain.RoutineItem;
import com.afterglow.domain.episode.routine.domain.RoutineRepository;
import com.afterglow.domain.episode.routine.domain.RoutineTimeSlot;
import com.afterglow.domain.tracking.daily.domain.ConditionLevel;
import com.afterglow.domain.tracking.daily.domain.DailyTracking;
import com.afterglow.domain.tracking.daily.domain.DailyTrackingRepository;
import com.afterglow.domain.tracking.daily.domain.SleepLevel;
import com.afterglow.domain.vanity.CombinationRule;
import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.OpeningPeriod;
import com.afterglow.domain.vanity.Product;
import com.afterglow.domain.vanity.RegistrationSource;
import com.afterglow.domain.vanity.UsageTiming;
import com.afterglow.domain.vanity.infrastructure.CombinationRuleRepository;
import com.afterglow.domain.vanity.infrastructure.ProductRepository;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.afterglow.global.exception.NotFoundException;

/**
 * 2026-08-20부터 Vanity/CheckIn/Routine/Tracking 실제 연동이 들어갔다 — raw data가 전혀 없으면 여전히
 * HOLD(NO_TARGET)이고(orchestration의 안전한 기본 동작), Vanity 제품/CheckIn/Routine/Sleep 이력을 실제로
 * 채우면 그 데이터를 근거로 candidate가 생성되는지까지 이 클래스에서 검증한다. Weather는 매칭 threshold
 * 미확정으로 여전히 candidate를 만들지 않는다(raw 조회/CheckIn 정렬은 별도 {@code WeatherObservationDayTest} 참고).
 */
@SpringBootTest
@Transactional
class EpisodeAnalysisServiceTest {

	@Autowired
	private EpisodeAnalysisService episodeAnalysisService;

	@Autowired
	private EpisodeRepository episodeRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CombinationRuleRepository combinationRuleRepository;

	@Autowired
	private CheckInRepository checkInRepository;

	@Autowired
	private RoutineRepository routineRepository;

	@Autowired
	private DailyTrackingRepository dailyTrackingRepository;

	@Test
	void 존재하지_않는_episode면_예외() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();

		assertThatThrownBy(() -> episodeAnalysisService.analyze(accountId, 999999L))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void 다른_account의_episode는_분석할_수_없다() {
		Long ownerId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = intakeCompletedEpisode(ownerId);

		Long otherId = accountRepository.save(Account.createAnonymous()).getId();

		assertThatThrownBy(() -> episodeAnalysisService.analyze(otherId, episodeId))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void intake를_끝내지_않은_episode는_분석할_수_없다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();

		assertThatThrownBy(() -> episodeAnalysisService.analyze(accountId, episodeId))
				.isInstanceOf(AfterglowException.class)
				.extracting(e -> ((AfterglowException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_EPISODE_STATE);
	}

	@Test
	void raw_data가_전혀_없으면_HOLD_NO_TARGET이_반환된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = intakeCompletedEpisode(accountId);

		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isTrue();
		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.NO_TARGET);
		assertThat(result.confidence()).isNull();
	}

	// ------------------------------------------------------------------
	// Vanity Product 실제 연동
	// ------------------------------------------------------------------

	@Test
	void RECENT_제품과_CheckIn_기록이_있으면_실제_PRODUCT_candidate가_채택된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		Long productId = saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of()).getId();
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(10));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		assertThat(result.cards().get(0).type()).isEqualTo(ResultCardType.DISCONTINUE);
		TimingEvidence evidence = (TimingEvidence) result.cards().get(0).evidence();
		assertThat(evidence.productId()).isEqualTo(productId);
		assertThat(evidence.usageStartDate()).isEqualTo(analysisDate.minusDays(14));
		// timing만 보면 STRONG이지만 OpeningPeriod 추정값이라 한 단계 downgrade되어 MEDIUM → confidence는 NORMAL.
		assertThat(result.confidence()).isEqualTo(com.afterglow.domain.episode.analysis.domain.Confidence.NORMAL);
	}

	/**
	 * onsetPeriod → symptomStartDate 환산 규칙이 아직 없어(후속 기획 확정 대기, Known limitation) 보수적으로
	 * 제한한다 — {@code RECENT_제품과_CheckIn_기록이_있으면_실제_PRODUCT_candidate가_채택된다}와 완전히 같은
	 * Vanity/CheckIn 데이터인데 onsetPeriod만 TODAY가 아니면, timing상 STRONG/MEDIUM이 나올 조건이어도 WEAK로
	 * 고정돼 HOLD가 된다(false positive 방지).
	 */
	@Test
	void onsetPeriod가_TODAY가_아니면_동일한_제품_데이터로도_HOLD가_된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of());
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(10));

		Long episodeId = intakeCompletedEpisode(accountId, OnsetPeriod.ONE_WEEK_AGO);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isTrue();
		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.INCONCLUSIVE_EVIDENCE);
	}

	@Test
	void openingPeriod가_없는_제품은_등록일_fallback으로_WEAK가_되어_HOLD이지만_NO_TARGET은_아니다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		// openingPeriod가 없으면 referenceDate가 제품 등록일(=오늘, persist 시점의 createdAt) 그대로라 window가 [오늘, 오늘]뿐이다.
		saveProduct(accountId, null, UsageTiming.MORNING, Set.of());
		saveCheckInOnAnyEpisode(accountId, analysisDate);

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isTrue();
		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.INCONCLUSIVE_EVIDENCE);
	}

	@Test
	void 제품은_있지만_CheckIn_기록이_없으면_INSUFFICIENT_RECORDS로_HOLD된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of());
		// CheckIn을 전혀 만들지 않는다 — coverageDays == 0.

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isTrue();
		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.INSUFFICIENT_RECORDS);
	}

	// ------------------------------------------------------------------
	// Routine lifecycle 반영
	// ------------------------------------------------------------------

	@Test
	void Routine이_제품을_중단시키면_그_이후_CheckIn은_coverage에_반영되지_않는다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		LocalDate referenceDate = analysisDate.minusDays(14); // RECENT
		Long productId = saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of()).getId();

		Long discontinueEpisodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		LocalDate discontinuedAt = referenceDate.plusDays(2);
		routineRepository.save(Routine.create(discontinueEpisodeId, accountId, discontinuedAt,
				List.of(RoutineItem.discontinueItem(productId))));

		// window은 referenceDate~discontinuedAt으로 잘리므로, 그 이후 날짜의 CheckIn은 coverage에 안 잡혀야 한다.
		saveCheckInOnAnyEpisode(accountId, discontinuedAt.plusDays(3));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isTrue();
		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.INSUFFICIENT_RECORDS);
	}

	@Test
	void 중단_후_재개돼도_원래_사용_시작일이_그대로_유지된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		LocalDate referenceDate = analysisDate.minusDays(14); // RECENT
		Long productId = saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of()).getId();

		Long discontinueEpisodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		routineRepository.save(Routine.create(discontinueEpisodeId, accountId, referenceDate.plusDays(2),
				List.of(RoutineItem.discontinueItem(productId))));

		Long resumeEpisodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		routineRepository.save(Routine.create(resumeEpisodeId, accountId, referenceDate.plusDays(5),
				List.of(RoutineItem.continueItem(1, RoutineTimeSlot.MORNING, productId))));

		// window은 referenceDate~referenceDate+2(중단일)로 잘리므로 그 안에 CheckIn을 둬야 candidate가 채택된다.
		saveCheckInOnAnyEpisode(accountId, referenceDate.plusDays(1));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		TimingEvidence evidence = (TimingEvidence) result.cards().get(0).evidence();
		assertThat(evidence.usageStartDate()).isEqualTo(referenceDate);
	}

	// ------------------------------------------------------------------
	// Vanity Combination 실제 연동
	// ------------------------------------------------------------------

	@Test
	void 충돌_태그를_가진_두_제품이_같은_시간대면_COMBINATION_candidate가_채택된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		combinationRuleRepository.save(CombinationRule.builder()
				.tagA(InteractionTag.RETINOL).tagB(InteractionTag.ACID).minCount(2).warningMessage("주의").build());
		// 두 제품 모두 timing이 어긋나 PRODUCT는 WEAK가 되도록 SIX_MONTHS_OR_MORE를 쓴다 — COMBINATION(STRONG)이 확실히 top이 되게 하기 위함.
		saveProduct(accountId, OpeningPeriod.SIX_MONTHS_OR_MORE, UsageTiming.MORNING, Set.of(InteractionTag.RETINOL));
		saveProduct(accountId, OpeningPeriod.SIX_MONTHS_OR_MORE, UsageTiming.MORNING, Set.of(InteractionTag.ACID));
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(175));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		assertThat(result.cards().get(0).causeType()).isEqualTo(com.afterglow.domain.episode.analysis.domain.CandidateType.COMBINATION);
	}

	// ------------------------------------------------------------------
	// CONTINUE_USE("오늘 사용할 것") 카드 — 2026-08-20 RC1 정책(LOW_IRRITATION 명시적 태그 기반)
	// ------------------------------------------------------------------

	@Test
	void LOW_IRRITATION_태그가_있고_PRODUCT_원인이_아니면_CONTINUE_USE에_포함된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		Long causeProductId = saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of()).getId();
		Long lowIrritationProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION)).getId();
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(10));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		assertThat(result.cards().get(0).causeType()).isEqualTo(com.afterglow.domain.episode.analysis.domain.CandidateType.PRODUCT);
		List<Long> continueUseProductIds = result.cards().get(1).continueUseProductIds();
		assertThat(continueUseProductIds).containsExactly(lowIrritationProductId);
		assertThat(continueUseProductIds).doesNotContain(causeProductId);
	}

	@Test
	void 원인_제품이면_LOW_IRRITATION_태그가_있어도_CONTINUE_USE에서_제외된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		Long causeProductId = saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of(InteractionTag.LOW_IRRITATION)).getId();
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(10));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		assertThat(result.cards().get(1).continueUseProductIds()).doesNotContain(causeProductId);
	}

	@Test
	void COMBINATION_원인이면_충돌_두_제품_모두_LOW_IRRITATION이어도_CONTINUE_USE에서_제외된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		combinationRuleRepository.save(CombinationRule.builder()
				.tagA(InteractionTag.RETINOL).tagB(InteractionTag.ACID).minCount(2).warningMessage("주의").build());
		Long retinolProductId = saveProduct(accountId, OpeningPeriod.SIX_MONTHS_OR_MORE, UsageTiming.MORNING,
				Set.of(InteractionTag.RETINOL, InteractionTag.LOW_IRRITATION)).getId();
		Long acidProductId = saveProduct(accountId, OpeningPeriod.SIX_MONTHS_OR_MORE, UsageTiming.MORNING,
				Set.of(InteractionTag.ACID, InteractionTag.LOW_IRRITATION)).getId();
		Long unrelatedLowIrritationProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION)).getId();
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(175));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		assertThat(result.cards().get(0).causeType()).isEqualTo(com.afterglow.domain.episode.analysis.domain.CandidateType.COMBINATION);
		List<Long> continueUseProductIds = result.cards().get(1).continueUseProductIds();
		assertThat(continueUseProductIds).containsExactly(unrelatedLowIrritationProductId);
		assertThat(continueUseProductIds).doesNotContain(retinolProductId, acidProductId);
	}

	@Test
	void RETINOL_ACID_VITAMIN_C_HIGH_CONCENTRATION_태그가_있으면_LOW_IRRITATION이_있어도_제외된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of()); // PRODUCT top cause 확보용
		Long retinolProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION, InteractionTag.RETINOL)).getId();
		Long acidProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION, InteractionTag.ACID)).getId();
		Long vitaminCProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION, InteractionTag.VITAMIN_C)).getId();
		Long highConcentrationProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION, InteractionTag.HIGH_CONCENTRATION)).getId();
		Long cleanLowIrritationProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION)).getId();
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(10));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		List<Long> continueUseProductIds = result.cards().get(1).continueUseProductIds();
		assertThat(continueUseProductIds).containsExactly(cleanLowIrritationProductId);
		assertThat(continueUseProductIds).doesNotContain(retinolProductId, acidProductId, vitaminCProductId, highConcentrationProductId);
	}

	@Test
	void Routine_STOP_상태인_제품은_LOW_IRRITATION이어도_CONTINUE_USE에서_제외된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of()); // PRODUCT top cause 확보용
		Long stoppedProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION)).getId();
		Long normalProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION)).getId();
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(10));

		Long discontinueEpisodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		routineRepository.save(Routine.create(discontinueEpisodeId, accountId, LocalDate.now().minusDays(30),
				List.of(RoutineItem.discontinueItem(stoppedProductId))));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		List<Long> continueUseProductIds = result.cards().get(1).continueUseProductIds();
		assertThat(continueUseProductIds).containsExactly(normalProductId);
		assertThat(continueUseProductIds).doesNotContain(stoppedProductId);
	}

	@Test
	void 중단_후_재개된_LOW_IRRITATION_제품은_다시_CONTINUE_USE에_포함된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of()); // PRODUCT top cause 확보용
		Long resumedProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION)).getId();
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(10));

		Long discontinueEpisodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		routineRepository.save(Routine.create(discontinueEpisodeId, accountId, LocalDate.now().minusDays(30),
				List.of(RoutineItem.discontinueItem(resumedProductId))));
		Long resumeEpisodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		routineRepository.save(Routine.create(resumeEpisodeId, accountId, LocalDate.now().minusDays(20),
				List.of(RoutineItem.continueItem(1, RoutineTimeSlot.MORNING, resumedProductId))));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.cards().get(1).continueUseProductIds()).contains(resumedProductId);
	}

	@Test
	void HOLD_상태면_CONTINUE_USE는_빈_배열이다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of(InteractionTag.LOW_IRRITATION));
		// CheckIn을 전혀 만들지 않아 coverage 0 → INSUFFICIENT_RECORDS → HOLD.

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isTrue();
		assertThat(result.cards().get(1).continueUseProductIds()).isEmpty();
	}

	@Test
	void SLEEP_원인이면_LOW_IRRITATION_제품이_있어도_CONTINUE_USE는_빈_배열이다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		saveProduct(accountId, null, UsageTiming.MORNING, Set.of(InteractionTag.LOW_IRRITATION));
		for (long i = 0; i < 7; i++) {
			LocalDate date = analysisDate.minusDays(i);
			saveDailyTracking(accountId, date, SleepLevel.WELL);
			saveCheckInOnAnyEpisode(accountId, date, CheckInStatus.IMPROVED);
		}

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		assertThat(result.cards().get(0).causeType()).isEqualTo(com.afterglow.domain.episode.analysis.domain.CandidateType.SLEEP);
		assertThat(result.cards().get(1).continueUseProductIds()).isEmpty();
	}

	@Test
	void Vanity_제품이_전혀_없으면_CONTINUE_USE는_빈_배열이다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.cards().get(1).continueUseProductIds()).isEmpty();
	}

	@Test
	void LOW_IRRITATION_태그를_가진_제품이_없으면_CONTINUE_USE는_빈_배열이다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of());
		saveProduct(accountId, null, UsageTiming.EVENING, Set.of());
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(10));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		assertThat(result.cards().get(1).continueUseProductIds()).isEmpty();
	}

	@Test
	void 다시_조회해도_CONTINUE_USE_목록은_동일한_규칙으로_일관되게_계산된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of());
		Long lowIrritationProductId = saveProduct(accountId, null, UsageTiming.EVENING, Set.of(InteractionTag.LOW_IRRITATION)).getId();
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(10));

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult analyzed = episodeAnalysisService.analyze(accountId, episodeId);
		ResultCardResult fetched = episodeAnalysisService.getResult(accountId, episodeId);

		assertThat(fetched.cards().get(1).continueUseProductIds())
				.containsExactly(lowIrritationProductId)
				.isEqualTo(analyzed.cards().get(1).continueUseProductIds());
	}

	// ------------------------------------------------------------------
	// Sleep 실제 연동
	// ------------------------------------------------------------------

	@Test
	void WELL_IMPROVED가_7일_모두_일치하면_SLEEP_candidate가_STRONG으로_채택된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		for (long i = 0; i < 7; i++) {
			LocalDate date = analysisDate.minusDays(i);
			saveDailyTracking(accountId, date, SleepLevel.WELL);
			saveCheckInOnAnyEpisode(accountId, date, CheckInStatus.IMPROVED);
		}

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		assertThat(result.cards().get(0).causeType()).isEqualTo(com.afterglow.domain.episode.analysis.domain.CandidateType.SLEEP);
		assertThat(result.confidence()).isEqualTo(com.afterglow.domain.episode.analysis.domain.Confidence.HIGH);
	}

	@Test
	void NORMAL_수면은_관측_대상에서_제외되어_CheckIn이_있어도_SLEEP_candidate가_생기지_않는다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		for (long i = 0; i < 7; i++) {
			LocalDate date = analysisDate.minusDays(i);
			saveDailyTracking(accountId, date, SleepLevel.NORMAL);
			saveCheckInOnAnyEpisode(accountId, date, CheckInStatus.IMPROVED);
		}

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isTrue();
		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.NO_TARGET);
	}

	/**
	 * 2026-08-20 정정 회귀 테스트(A) — 관측 2건이 calendar로는 7일 떨어져 있어도(8/1·8/7 스타일) coverage는
	 * calendar span(7)이 아니라 실제 관측 건수(2)여야 한다. calendar span 기준이었다면 이 테스트가 통과했을
	 * 것이므로, 이 테스트가 실패하지 않는 것 자체가 "더 이상 calendar span을 쓰지 않는다"는 증거다.
	 */
	@Test
	void coverage는_calendar_span이_아니라_실제_관측_건수다_A() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		LocalDate day1 = analysisDate.minusDays(6);
		LocalDate day7 = analysisDate; // day1~day7 calendar span = 7이지만 실제 관측은 이 두 날짜뿐 → 2건.
		saveDailyTracking(accountId, day1, SleepLevel.WELL);
		saveCheckInOnAnyEpisode(accountId, day1, CheckInStatus.IMPROVED);
		saveDailyTracking(accountId, day7, SleepLevel.WELL);
		saveCheckInOnAnyEpisode(accountId, day7, CheckInStatus.IMPROVED);

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isTrue();
		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.INSUFFICIENT_RECORDS);
	}

	/** 회귀 테스트(B) — 서로 다른 7개 날짜에 관측이 있으면 coverageDays=7이고, 일부만 일치해도(matched &lt; coverage, 회귀 테스트 D) candidate가 채택된다. */
	@Test
	void 서로_다른_7개_날짜에_관측이_있으면_coverageDays는_7이고_matched는_coverage를_넘지_않는다_B_D() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		for (long i = 0; i < 7; i++) {
			LocalDate date = analysisDate.minusDays(i);
			saveDailyTracking(accountId, date, SleepLevel.WELL);
			// 4일은 IMPROVED(일치), 3일은 SAME(불일치) → matched=4 < coverage=7.
			saveCheckInOnAnyEpisode(accountId, date, i < 4 ? CheckInStatus.IMPROVED : CheckInStatus.SAME);
		}

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		assertThat(result.cards().get(0).coverageDays()).isEqualTo(7L);
		FrequencyEvidence evidence = (FrequencyEvidence) result.cards().get(0).evidence();
		assertThat(evidence.observationCount()).isEqualTo(7);
		assertThat(evidence.matchedObservationCount()).isEqualTo(4);
		assertThat(evidence.matchedObservationCount()).isLessThanOrEqualTo(evidence.observationCount());
	}

	/** 회귀 테스트(C) — 7일 동안 Tracking은 매일 있어도 그중 3일이 NORMAL이면 실제 관측 가능일은 4일뿐이라 coverage 게이트를 통과하지 못한다. */
	@Test
	void NORMAL을_제외한_실제_관측_가능일만_coverage에_포함된다_C() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();
		for (long i = 0; i < 7; i++) {
			LocalDate date = analysisDate.minusDays(i);
			SleepLevel sleepLevel = i < 3 ? SleepLevel.NORMAL : SleepLevel.WELL;
			saveDailyTracking(accountId, date, sleepLevel);
			saveCheckInOnAnyEpisode(accountId, date, CheckInStatus.IMPROVED);
		}

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		// 실제 관측 가능일 = 4일(NORMAL 3일 제외) < 7 → INSUFFICIENT_RECORDS.
		assertThat(result.hold()).isTrue();
		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.INSUFFICIENT_RECORDS);
	}

	// ------------------------------------------------------------------
	// Product + Combination + Sleep 동시 연동(회귀 방지용 통합 시나리오)
	// ------------------------------------------------------------------

	@Test
	void Product_Sleep_raw_데이터가_모두_있으면_NO_TARGET이_아닌_결과가_나온다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		LocalDate analysisDate = LocalDate.now();

		saveProduct(accountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of());
		saveCheckInOnAnyEpisode(accountId, analysisDate.minusDays(10), CheckInStatus.SAME);

		for (long i = 0; i < 7; i++) {
			LocalDate date = analysisDate.minusDays(i);
			saveDailyTracking(accountId, date, SleepLevel.WELL);
			saveCheckInOnAnyEpisode(accountId, date, CheckInStatus.IMPROVED);
		}

		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isFalse();
		assertThat(result.holdReason()).isNull();
	}

	// ------------------------------------------------------------------
	// account 간 데이터 격리
	// ------------------------------------------------------------------

	@Test
	void 다른_account의_Vanity_CheckIn_데이터는_섞이지_않는다() {
		Long otherAccountId = accountRepository.save(Account.createAnonymous()).getId();
		saveProduct(otherAccountId, OpeningPeriod.RECENT, UsageTiming.MORNING, Set.of());
		saveCheckInOnAnyEpisode(otherAccountId, LocalDate.now().minusDays(10));

		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = intakeCompletedEpisode(accountId);
		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isTrue();
		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.NO_TARGET);
	}

	@Test
	void 분석_결과는_항상_카드_3장이고_첫_카드는_WITHHELD다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = intakeCompletedEpisode(accountId);

		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.cards()).hasSize(3);
		assertThat(result.cards().get(0).type()).isEqualTo(ResultCardType.WITHHELD);
		assertThat(result.cards().get(1).type()).isEqualTo(ResultCardType.CONTINUE_USE);
		assertThat(result.cards().get(2).type()).isEqualTo(ResultCardType.HOSPITAL_VISIT);
	}

	@Test
	void 분석에_성공하면_episode_상태가_ANALYZED로_전이된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = intakeCompletedEpisode(accountId);

		episodeAnalysisService.analyze(accountId, episodeId);

		Episode reloaded = episodeRepository.findByIdAndAccountId(episodeId, accountId).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(EpisodeStatus.ANALYZED);
	}

	@Test
	void 이미_분석된_episode는_재실행하지_않고_같은_결과를_그대로_반환한다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = intakeCompletedEpisode(accountId);

		ResultCardResult first = episodeAnalysisService.analyze(accountId, episodeId);
		ResultCardResult second = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(second).isEqualTo(first);
	}

	@Test
	void 분석_전에_결과를_조회하면_예외() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = intakeCompletedEpisode(accountId);

		assertThatThrownBy(() -> episodeAnalysisService.getResult(accountId, episodeId))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void 분석_후_결과_조회는_analyze_반환값과_동일하다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = intakeCompletedEpisode(accountId);

		ResultCardResult analyzed = episodeAnalysisService.analyze(accountId, episodeId);
		ResultCardResult fetched = episodeAnalysisService.getResult(accountId, episodeId);

		assertThat(fetched).isEqualTo(analyzed);
	}

	@Test
	void 다른_account는_결과를_조회할_수_없다() {
		Long ownerId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = intakeCompletedEpisode(ownerId);
		episodeAnalysisService.analyze(ownerId, episodeId);

		Long otherId = accountRepository.save(Account.createAnonymous()).getId();

		assertThatThrownBy(() -> episodeAnalysisService.getResult(otherId, episodeId))
				.isInstanceOf(NotFoundException.class);
	}

	private Long intakeCompletedEpisode(Long accountId) {
		return intakeCompletedEpisode(accountId, OnsetPeriod.TODAY);
	}

	private Long intakeCompletedEpisode(Long accountId, OnsetPeriod onsetPeriod) {
		Episode episode = episodeRepository.save(Episode.create(accountId, symptom()));
		episode.submitIntake(intake(onsetPeriod), java.util.Set.of(BodyPart.CHEEK));
		return episode.getId();
	}

	private Symptom symptom() {
		return Symptom.create(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE);
	}

	private Intake intake(OnsetPeriod onsetPeriod) {
		return Intake.create(onsetPeriod, "새 앰플", null);
	}

	private Product saveProduct(Long accountId, OpeningPeriod openingPeriod, UsageTiming usageTiming, Set<InteractionTag> interactionTags) {
		return productRepository.save(Product.builder()
				.accountId(accountId)
				.name("테스트 제품")
				.openingPeriod(openingPeriod)
				.usageTiming(usageTiming)
				.interactionTags(interactionTags)
				.registrationSource(RegistrationSource.MANUAL)
				.build());
	}

	/** CheckIn은 계정의 아무 Episode에나 기록해도 Analysis의 coverage 계산에는 동일하게 반영된다(account/date 기준 조회이기 때문). */
	private void saveCheckInOnAnyEpisode(Long accountId, LocalDate checkInDate) {
		saveCheckInOnAnyEpisode(accountId, checkInDate, CheckInStatus.SAME);
	}

	private void saveCheckInOnAnyEpisode(Long accountId, LocalDate checkInDate, CheckInStatus status) {
		Long episodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		checkInRepository.save(CheckIn.create(episodeId, checkInDate, status));
	}

	private void saveDailyTracking(Long accountId, LocalDate recordedDate, SleepLevel sleepLevel) {
		dailyTrackingRepository.save(DailyTracking.create(
				accountId, recordedDate, sleepLevel, ConditionLevel.NORMAL, 25.0, 18.0, 50.0, 5.0));
	}
}
