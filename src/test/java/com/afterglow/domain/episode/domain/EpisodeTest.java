package com.afterglow.domain.episode.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

class EpisodeTest {

	@Test
	void 생성_직후에는_SYMPTOM_SELECTED_상태다() {
		Episode episode = Episode.create(1L, symptom());

		assertThat(episode.getStatus()).isEqualTo(EpisodeStatus.SYMPTOM_SELECTED);
		assertThat(episode.getAccountId()).isEqualTo(1L);
	}

	@Test
	void 문진을_제출하면_INTAKE_COMPLETED로_전이한다() {
		Episode episode = Episode.create(1L, symptom());

		episode.submitIntake(intake(), Set.of(BodyPart.CHEEK));

		assertThat(episode.getStatus()).isEqualTo(EpisodeStatus.INTAKE_COMPLETED);
		assertThat(episode.getBodyParts()).containsExactly(BodyPart.CHEEK);
	}

	@Test
	void 이미_문진이_완료된_에피소드에_다시_제출하면_예외가_발생한다() {
		Episode episode = Episode.create(1L, symptom());
		episode.submitIntake(intake(), Set.of(BodyPart.CHEEK));

		assertThatThrownBy(() -> episode.submitIntake(intake(), Set.of(BodyPart.CHIN)))
				.isInstanceOf(AfterglowException.class)
				.satisfies(e -> assertThat(((AfterglowException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_EPISODE_STATE));
	}

	@Test
	void 전체_부위와_다른_부위를_동시에_선택하면_예외가_발생한다() {
		Episode episode = Episode.create(1L, symptom());

		assertThatThrownBy(() -> episode.submitIntake(intake(), Set.of(BodyPart.WHOLE_FACE, BodyPart.CHEEK)))
				.isInstanceOf(AfterglowException.class)
				.satisfies(e -> assertThat(((AfterglowException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
	}

	@Test
	void 전체_부위만_단독으로_선택하는_것은_허용한다() {
		Episode episode = Episode.create(1L, symptom());

		episode.submitIntake(intake(), Set.of(BodyPart.WHOLE_FACE));

		assertThat(episode.getBodyParts()).containsExactly(BodyPart.WHOLE_FACE);
	}

	private Symptom symptom() {
		return Symptom.create(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE);
	}

	private Intake intake() {
		return Intake.create(OnsetPeriod.TODAY, "새로 산 앰플", null);
	}
}
