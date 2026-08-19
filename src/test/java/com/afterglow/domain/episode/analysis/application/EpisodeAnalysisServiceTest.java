package com.afterglow.domain.episode.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.episode.card.domain.ResultCardHoldReason;
import com.afterglow.domain.episode.card.domain.ResultCardResult;
import com.afterglow.domain.episode.card.domain.ResultCardType;
import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.EpisodeStatus;
import com.afterglow.domain.episode.domain.Intake;
import com.afterglow.domain.episode.domain.OnsetPeriod;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.domain.Symptom;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.afterglow.global.exception.NotFoundException;

/**
 * Vanity/Tracking 실제 연동이 없는(2026-08-19 기준 dev) 지금 상태에서, orchestration 자체가 안전하게
 * 동작하는지 검증한다 — raw data가 전부 없으니 항상 HOLD(NO_TARGET)가 나오는 게 "정상"이다.
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
	void 정상_episode를_분석하면_Vanity_Tracking_데이터가_없어_HOLD_NO_TARGET이_반환된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = intakeCompletedEpisode(accountId);

		ResultCardResult result = episodeAnalysisService.analyze(accountId, episodeId);

		assertThat(result.hold()).isTrue();
		assertThat(result.holdReason()).isEqualTo(ResultCardHoldReason.NO_TARGET);
		assertThat(result.confidence()).isNull();
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
		Episode episode = episodeRepository.save(Episode.create(accountId, symptom()));
		episode.submitIntake(intake(), java.util.Set.of(BodyPart.CHEEK));
		return episode.getId();
	}

	private Symptom symptom() {
		return Symptom.create(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE);
	}

	private Intake intake() {
		return Intake.create(OnsetPeriod.TODAY, "새 앰플", null);
	}
}
