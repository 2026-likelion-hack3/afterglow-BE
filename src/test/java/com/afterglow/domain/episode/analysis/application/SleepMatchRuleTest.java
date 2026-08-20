package com.afterglow.domain.episode.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.tracking.daily.domain.SleepLevel;

class SleepMatchRuleTest {

	@Test
	void WELL_IMPROVED는_일치한다() {
		assertThat(SleepMatchRule.matches(SleepLevel.WELL, CheckInStatus.IMPROVED)).isTrue();
	}

	@Test
	void WELL_SAME은_일치하지_않는다() {
		assertThat(SleepMatchRule.matches(SleepLevel.WELL, CheckInStatus.SAME)).isFalse();
	}

	@Test
	void WELL_WORSE는_일치하지_않는다() {
		assertThat(SleepMatchRule.matches(SleepLevel.WELL, CheckInStatus.WORSE)).isFalse();
	}

	@Test
	void POOR_SAME은_일치한다() {
		assertThat(SleepMatchRule.matches(SleepLevel.POOR, CheckInStatus.SAME)).isTrue();
	}

	@Test
	void POOR_WORSE는_일치한다() {
		assertThat(SleepMatchRule.matches(SleepLevel.POOR, CheckInStatus.WORSE)).isTrue();
	}

	@Test
	void POOR_IMPROVED는_일치하지_않는다() {
		assertThat(SleepMatchRule.matches(SleepLevel.POOR, CheckInStatus.IMPROVED)).isFalse();
	}

	@Test
	void NORMAL은_관측_대상이_아니다() {
		assertThat(SleepMatchRule.isObservable(SleepLevel.NORMAL)).isFalse();
	}

	@Test
	void WELL과_POOR는_관측_대상이다() {
		assertThat(SleepMatchRule.isObservable(SleepLevel.WELL)).isTrue();
		assertThat(SleepMatchRule.isObservable(SleepLevel.POOR)).isTrue();
	}
}
