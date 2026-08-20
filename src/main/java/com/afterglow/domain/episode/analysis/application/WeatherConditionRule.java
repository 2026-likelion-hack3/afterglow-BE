package com.afterglow.domain.episode.analysis.application;

/**
 * Weather observation의 "그날 날씨 조건이 충족됐는가"(conditionMet) 판정 기준(2026-08-21 기획 확정).
 * 아래 세 그룹 중 하나라도 충족하면 conditionMet=true(그룹 간 OR):
 * <ol>
 *   <li>평균습도 40% 미만, 또는 전날 대비 평균습도 15%p 이상 하락</li>
 *   <li>전날 대비 평균기온 5°C 이상 하락, 또는 당일 최저기온 5°C 미만</li>
 *   <li>당일 또는 전날 최대 UV 6 이상</li>
 * </ol>
 * "전날 대비" 항목은 전날 raw 값이 없으면(관측 공백 등) 그 항목만 미충족으로 보고 나머지 항목으로 계속
 * 판단한다. {@link WeatherMatchRule}(conditionMet + CheckIn → 일치 여부)과는 책임이 분리돼 있다 — 이
 * 클래스는 conditionMet 자체를 계산한다.
 */
public final class WeatherConditionRule {

	private static final double HUMIDITY_LOW_THRESHOLD = 40.0;
	private static final double HUMIDITY_DROP_THRESHOLD = 15.0;
	private static final double TEMPERATURE_DROP_THRESHOLD = 5.0;
	private static final double LOW_TEMPERATURE_THRESHOLD = 5.0;
	private static final double UV_THRESHOLD = 6.0;

	private WeatherConditionRule() {
	}

	/** yesterday가 없으면(null) "전날 대비" 항목은 미충족으로 본다. */
	public static boolean conditionMet(WeatherObservationDay today, WeatherObservationDay yesterday) {
		return humidityConditionMet(today, yesterday)
				|| temperatureConditionMet(today, yesterday)
				|| uvConditionMet(today, yesterday);
	}

	private static boolean humidityConditionMet(WeatherObservationDay today, WeatherObservationDay yesterday) {
		if (isBelow(today.humidity(), HUMIDITY_LOW_THRESHOLD)) {
			return true;
		}
		return droppedAtLeast(previousValue(yesterday, WeatherObservationDay::humidity), today.humidity(), HUMIDITY_DROP_THRESHOLD);
	}

	private static boolean temperatureConditionMet(WeatherObservationDay today, WeatherObservationDay yesterday) {
		if (droppedAtLeast(previousValue(yesterday, WeatherObservationDay::temperature), today.temperature(), TEMPERATURE_DROP_THRESHOLD)) {
			return true;
		}
		return isBelow(today.minTemperature(), LOW_TEMPERATURE_THRESHOLD);
	}

	private static boolean uvConditionMet(WeatherObservationDay today, WeatherObservationDay yesterday) {
		if (isAtLeast(today.uvIndex(), UV_THRESHOLD)) {
			return true;
		}
		return isAtLeast(previousValue(yesterday, WeatherObservationDay::uvIndex), UV_THRESHOLD);
	}

	private static Double previousValue(WeatherObservationDay yesterday, java.util.function.Function<WeatherObservationDay, Double> extractor) {
		return yesterday == null ? null : extractor.apply(yesterday);
	}

	private static boolean isBelow(Double value, double threshold) {
		return value != null && value < threshold;
	}

	private static boolean isAtLeast(Double value, double threshold) {
		return value != null && value >= threshold;
	}

	private static boolean droppedAtLeast(Double previous, Double current, double threshold) {
		if (previous == null || current == null) {
			return false;
		}
		return previous - current >= threshold;
	}
}
