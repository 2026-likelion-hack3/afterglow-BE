package com.afterglow.domain.episode.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.episode.analysis.domain.AnalysisExplanation;
import com.afterglow.domain.episode.analysis.domain.AnalysisExplanationGenerator;
import com.afterglow.domain.episode.analysis.domain.AnalysisExplanationOutcome;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResult;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResultRepository;
import com.afterglow.domain.episode.analysis.domain.ExplanationSource;
import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.Intake;
import com.afterglow.domain.episode.domain.OnsetPeriod;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.domain.Symptom;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.afterglow.global.exception.NotFoundException;

/**
 * 실제 OpenAI를 호출하지 않는다 — {@link AnalysisExplanationGenerator}는 람다로 직접 구현해 원하는
 * 성공/실패를 결정적으로 재현한다. Repository는 실제 DB를 그대로 쓴다(이 프로젝트의 기존 테스트
 * 관례와 동일) — {@link EpisodeAnalysisExplanationService}만 수동으로 생성해 generator를 바꿔가며
 * 검증한다.
 */
@SpringBootTest
@Transactional
class EpisodeAnalysisExplanationServiceTest {

	@Autowired
	private EpisodeAnalysisService episodeAnalysisService;

	@Autowired
	private EpisodeRepository episodeRepository;

	@Autowired
	private EpisodeAnalysisResultRepository episodeAnalysisResultRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Test
	void 정상_AI_generator이면_source_AI와_구조화된_explanation을_반환한다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = analyzedEpisode(accountId);

		AnalysisExplanationGenerator generator = analysisResult ->
				new AnalysisExplanation("제목", "요약", List.of("근거1", "근거2"), "다음 행동");
		EpisodeAnalysisExplanationService service = serviceWith(Optional.of(generator));

		AnalysisExplanationOutcome outcome = service.explain(accountId, episodeId);

		assertThat(outcome.source()).isEqualTo(ExplanationSource.AI);
		assertThat(outcome.explanation().headline()).isEqualTo("제목");
		assertThat(outcome.explanation().evidenceNotes()).containsExactly("근거1", "근거2");
	}

	@Test
	void generator가_AI_REQUEST_FAILED로_실패하면_fallback으로_전환하고_예외를_던지지_않는다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = analyzedEpisode(accountId);

		AnalysisExplanationGenerator failingGenerator = analysisResult -> {
			throw new AfterglowException(ErrorCode.AI_REQUEST_FAILED);
		};
		EpisodeAnalysisExplanationService service = serviceWith(Optional.of(failingGenerator));

		AnalysisExplanationOutcome outcome = service.explain(accountId, episodeId);

		assertThat(outcome.source()).isEqualTo(ExplanationSource.FALLBACK);
	}

	@Test
	void generator가_없으면_fallback을_쓴다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = analyzedEpisode(accountId);

		EpisodeAnalysisExplanationService service = serviceWith(Optional.empty());

		AnalysisExplanationOutcome outcome = service.explain(accountId, episodeId);

		assertThat(outcome.source()).isEqualTo(ExplanationSource.FALLBACK);
	}

	@Test
	void AI_REQUEST_FAILED가_아닌_예외는_그대로_전파된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = analyzedEpisode(accountId);

		AnalysisExplanationGenerator buggyGenerator = analysisResult -> {
			throw new IllegalStateException("버그");
		};
		EpisodeAnalysisExplanationService service = serviceWith(Optional.of(buggyGenerator));

		assertThatThrownBy(() -> service.explain(accountId, episodeId))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void hold_결과에서_fallback은_불확실성을_유지한다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = analyzedEpisode(accountId); // Vanity/Tracking 미연동이라 항상 HOLD(NO_TARGET)

		EpisodeAnalysisExplanationService service = serviceWith(Optional.empty());

		AnalysisExplanationOutcome outcome = service.explain(accountId, episodeId);

		assertThat(outcome.explanation().headline()).contains("어려워요");
		assertThat(outcome.explanation().summary()).doesNotContain("확정했");
	}

	@Test
	void 분석_결과가_없는_episode는_예외() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Episode episode = episodeRepository.save(Episode.create(accountId, symptom()));
		episode.submitIntake(intake(), Set.of(BodyPart.CHEEK));
		// analyze()를 호출하지 않아 EpisodeAnalysisResult가 없다.

		EpisodeAnalysisExplanationService service = serviceWith(Optional.empty());

		assertThatThrownBy(() -> service.explain(accountId, episode.getId()))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void 다른_account는_explanation을_조회할_수_없다() {
		Long ownerId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = analyzedEpisode(ownerId);
		Long otherId = accountRepository.save(Account.createAnonymous()).getId();

		EpisodeAnalysisExplanationService service = serviceWith(Optional.empty());

		assertThatThrownBy(() -> service.explain(otherId, episodeId))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void explanation_호출은_저장된_분석_결과를_바꾸지_않는다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = analyzedEpisode(accountId);

		EpisodeAnalysisResult before = episodeAnalysisResultRepository
				.findByEpisodeIdAndAccountId(episodeId, accountId).orElseThrow();
		boolean holdBefore = before.isHold();
		var holdReasonBefore = before.getHoldReason();
		var confidenceBefore = before.getConfidence();
		var cardTypeBefore = before.getCardType();

		AnalysisExplanationGenerator generator = analysisResult ->
				new AnalysisExplanation("제목", "요약", List.of("근거"), "다음");
		serviceWith(Optional.of(generator)).explain(accountId, episodeId);

		EpisodeAnalysisResult after = episodeAnalysisResultRepository
				.findByEpisodeIdAndAccountId(episodeId, accountId).orElseThrow();
		assertThat(after.isHold()).isEqualTo(holdBefore);
		assertThat(after.getHoldReason()).isEqualTo(holdReasonBefore);
		assertThat(after.getConfidence()).isEqualTo(confidenceBefore);
		assertThat(after.getCardType()).isEqualTo(cardTypeBefore);
		assertThat(after.getId()).isEqualTo(before.getId());
	}

	private EpisodeAnalysisExplanationService serviceWith(Optional<AnalysisExplanationGenerator> generator) {
		return new EpisodeAnalysisExplanationService(episodeRepository, episodeAnalysisResultRepository, generator);
	}

	private Long analyzedEpisode(Long accountId) {
		Episode episode = episodeRepository.save(Episode.create(accountId, symptom()));
		episode.submitIntake(intake(), Set.of(BodyPart.CHEEK));
		episodeAnalysisService.analyze(accountId, episode.getId());
		return episode.getId();
	}

	private Symptom symptom() {
		return Symptom.create(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE);
	}

	private Intake intake() {
		return Intake.create(OnsetPeriod.TODAY, "새 앰플", null);
	}
}
