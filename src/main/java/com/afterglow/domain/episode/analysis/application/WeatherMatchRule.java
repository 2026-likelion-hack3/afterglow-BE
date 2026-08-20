package com.afterglow.domain.episode.analysis.application;

import com.afterglow.domain.episode.checkin.domain.CheckInStatus;

/**
 * Weather observation의 "그날 날씨와 증상이 일치하는가" 판정 규칙(2026-08-20 기획 확정). 조건 충족
 * + WORSE/SAME → MATCH, 조건 충족 + IMPROVED → MISMATCH. 조건 미충족 + IMPROVED → MATCH, 조건 미충족
 * + SAME/WORSE → MISMATCH. 즉 conditionMet과 "좋아지지 않음(IMPROVED가 아님)"이 같은 방향일 때 일치한다.
 *
 * <p><b>conditionMet은 이 클래스의 책임이 아니다</b> — 온도/습도/자외선 중 무엇을, 어떤 값으로 "날씨 조건
 * 충족"으로 볼지의 threshold는 아직 Manyfast에 없다(docs/domains/episode.md Pending Decisions). 그래서
 * conditionMet은 이미 판정된 boolean으로만 받는다. threshold가 확정되면 {@link WeatherObservationDay}를
 * 순회하며 그 threshold로 conditionMet을 계산한 뒤 이 메서드에 넘기면 된다 — 이 메서드 자체는 바꿀 필요가 없다.
 */
public final class WeatherMatchRule {

	private WeatherMatchRule() {
	}

	public static boolean matches(boolean conditionMet, CheckInStatus checkInStatus) {
		boolean improved = checkInStatus == CheckInStatus.IMPROVED;
		return conditionMet != improved;
	}
}
