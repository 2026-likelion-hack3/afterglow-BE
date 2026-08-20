package com.afterglow.domain.episode.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.afterglow.domain.episode.checkin.domain.CheckInStatus;

class WeatherMatchRuleTest {

	@Test
	void 조건_충족_WORSE는_일치한다() {
		assertThat(WeatherMatchRule.matches(true, CheckInStatus.WORSE)).isTrue();
	}

	@Test
	void 조건_충족_SAME은_일치한다() {
		assertThat(WeatherMatchRule.matches(true, CheckInStatus.SAME)).isTrue();
	}

	@Test
	void 조건_충족_IMPROVED는_일치하지_않는다() {
		assertThat(WeatherMatchRule.matches(true, CheckInStatus.IMPROVED)).isFalse();
	}

	@Test
	void 조건_미충족_IMPROVED는_일치한다() {
		assertThat(WeatherMatchRule.matches(false, CheckInStatus.IMPROVED)).isTrue();
	}

	@Test
	void 조건_미충족_WORSE는_일치하지_않는다() {
		assertThat(WeatherMatchRule.matches(false, CheckInStatus.WORSE)).isFalse();
	}

	@Test
	void 조건_미충족_SAME은_일치하지_않는다() {
		assertThat(WeatherMatchRule.matches(false, CheckInStatus.SAME)).isFalse();
	}
}
