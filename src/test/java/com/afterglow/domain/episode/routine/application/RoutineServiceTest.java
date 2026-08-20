package com.afterglow.domain.episode.routine.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.domain.Symptom;
import com.afterglow.domain.episode.routine.domain.ProductUsageEvent;
import com.afterglow.domain.episode.routine.domain.Routine;
import com.afterglow.domain.episode.routine.domain.RoutineItem;
import com.afterglow.domain.episode.routine.domain.RoutineItemUsage;
import com.afterglow.domain.episode.routine.domain.RoutineRepository;
import com.afterglow.domain.episode.routine.domain.RoutineTimeSlot;

/**
 * 기획 확정(2026-08-19)에 따른 제품 중단/재개를, Episode를 넘나드는 계정 전체 Routine 이력에서
 * 실제로 조회할 수 있는지 검증한다(persistence 포함). Vanity Repository/Service는 이 프로젝트에
 * 아직 존재하지 않으므로(dev 기준), 이 테스트는 Vanity를 전혀 참조하지 않는다는 것 자체가 "앱에 의한
 * 재개가 Vanity/Analysis 기준일에 손대지 않는다"는 경계를 구조적으로 보여준다.
 */
@SpringBootTest
@Transactional
class RoutineServiceTest {

	private static final long PRODUCT_ID = 100L;

	@Autowired
	private RoutineService routineService;

	@Autowired
	private RoutineRepository routineRepository;

	@Autowired
	private EpisodeRepository episodeRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Test
	void 다른_Episode의_Routine에서_같은_제품이_중단됐다가_재개되면_이력으로_확인된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();

		Long firstEpisodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		routineRepository.save(Routine.create(firstEpisodeId, accountId, LocalDate.of(2026, 8, 1),
				List.of(RoutineItem.discontinueItem(PRODUCT_ID))));

		Long secondEpisodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		routineRepository.save(Routine.create(secondEpisodeId, accountId, LocalDate.of(2026, 8, 15),
				List.of(RoutineItem.continueItem(1, RoutineTimeSlot.MORNING, PRODUCT_ID))));

		List<ProductUsageEvent> events = routineService.productLifecycle(accountId, PRODUCT_ID);

		assertThat(events).hasSize(2);
		assertThat(events.get(0).usage()).isEqualTo(RoutineItemUsage.DISCONTINUE);
		assertThat(events.get(1).usage()).isEqualTo(RoutineItemUsage.CONTINUE);
		assertThat(events.get(1).resumed()).isTrue();
		assertThat(events.get(1).date()).isEqualTo(LocalDate.of(2026, 8, 15));
	}

	@Test
	void 재개_조회_이후에도_최초_중단_Routine의_기록은_그대로_남아있다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();

		Long firstEpisodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		Long routineId = routineRepository.save(Routine.create(firstEpisodeId, accountId, LocalDate.of(2026, 8, 1),
				List.of(RoutineItem.discontinueItem(PRODUCT_ID)))).getId();

		Long secondEpisodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		routineRepository.save(Routine.create(secondEpisodeId, accountId, LocalDate.of(2026, 8, 15),
				List.of(RoutineItem.continueItem(1, RoutineTimeSlot.MORNING, PRODUCT_ID))));

		routineService.productLifecycle(accountId, PRODUCT_ID);

		Routine reloaded = routineRepository.findByEpisodeIdAndAccountId(firstEpisodeId, accountId).orElseThrow();
		assertThat(reloaded.getId()).isEqualTo(routineId);
		assertThat(reloaded.discontinuedProductIds()).containsExactly(PRODUCT_ID);
		assertThat(reloaded.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
	}

	private Symptom symptom() {
		return Symptom.create(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE);
	}
}
