package com.afterglow.domain.episode.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class WeatherConditionRuleTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 8, 21);
	private static final LocalDate YESTERDAY = TODAY.minusDays(1);

	@Test
	void 평균습도가_40_미만이면_충족() {
		WeatherObservationDay today = day(TODAY, 25.0, 18.0, 39.9, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, null)).isTrue();
	}

	@Test
	void 평균습도가_40_이상이면_습도_단독으로는_미충족() {
		WeatherObservationDay today = day(TODAY, 25.0, 18.0, 40.0, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, null)).isFalse();
	}

	@Test
	void 전날_대비_평균습도가_15퍼센트포인트_이상_하락하면_충족() {
		WeatherObservationDay yesterday = day(YESTERDAY, 25.0, 18.0, 60.0, 3.0);
		WeatherObservationDay today = day(TODAY, 25.0, 18.0, 45.0, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, yesterday)).isTrue();
	}

	@Test
	void 전날_대비_평균습도_하락이_15퍼센트포인트_미만이면_미충족() {
		WeatherObservationDay yesterday = day(YESTERDAY, 25.0, 18.0, 60.0, 3.0);
		WeatherObservationDay today = day(TODAY, 25.0, 18.0, 46.0, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, yesterday)).isFalse();
	}

	@Test
	void 전날_대비_평균기온이_5도_이상_하락하면_충족() {
		WeatherObservationDay yesterday = day(YESTERDAY, 25.0, 18.0, 50.0, 3.0);
		WeatherObservationDay today = day(TODAY, 20.0, 18.0, 50.0, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, yesterday)).isTrue();
	}

	@Test
	void 당일_최저기온이_5도_미만이면_충족() {
		WeatherObservationDay today = day(TODAY, 25.0, 4.9, 50.0, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, null)).isTrue();
	}

	@Test
	void 당일_최저기온이_5도_이상이면_기온_단독으로는_미충족() {
		WeatherObservationDay today = day(TODAY, 25.0, 5.0, 50.0, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, null)).isFalse();
	}

	@Test
	void 당일_최대_UV가_6_이상이면_충족() {
		WeatherObservationDay today = day(TODAY, 25.0, 18.0, 50.0, 6.0);

		assertThat(WeatherConditionRule.conditionMet(today, null)).isTrue();
	}

	@Test
	void 전날_최대_UV가_6_이상이면_충족() {
		WeatherObservationDay yesterday = day(YESTERDAY, 25.0, 18.0, 50.0, 6.0);
		WeatherObservationDay today = day(TODAY, 25.0, 18.0, 50.0, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, yesterday)).isTrue();
	}

	@Test
	void 어떤_조건도_충족하지_않으면_미충족() {
		WeatherObservationDay yesterday = day(YESTERDAY, 25.0, 18.0, 50.0, 3.0);
		WeatherObservationDay today = day(TODAY, 25.0, 18.0, 50.0, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, yesterday)).isFalse();
	}

	@Test
	void 전날_데이터가_없어도_당일_값만으로_충족될_수_있다() {
		WeatherObservationDay today = day(TODAY, 25.0, 18.0, 30.0, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, null)).isTrue();
	}

	@Test
	void 전날_데이터가_없으면_전날_대비_항목은_미충족으로_처리되어_다른_조건_없으면_전체_미충족() {
		WeatherObservationDay today = day(TODAY, 25.0, 18.0, 50.0, 3.0);

		assertThat(WeatherConditionRule.conditionMet(today, null)).isFalse();
	}

	private WeatherObservationDay day(LocalDate date, Double temperature, Double minTemperature, Double humidity, Double uvIndex) {
		return new WeatherObservationDay(date, temperature, minTemperature, humidity, uvIndex, null);
	}
}
