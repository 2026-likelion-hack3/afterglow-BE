package com.afterglow.domain.episode.analysis.application;

import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.tracking.daily.domain.SleepLevel;

/**
 * Sleep observation의 "그날 수면과 증상이 일치하는가" 판정 규칙(2026-08-20 확정). CheckIn 상태를
 * GOOD(IMPROVED)/BAD(SAME, WORSE) 둘로 압축한 뒤: WELL+GOOD, POOR+BAD를 일치로 본다. NORMAL은 판정
 * 대상 자체가 아니다 — {@link #isObservable}로 먼저 걸러야 한다.
 */
public final class SleepMatchRule {

	private SleepMatchRule() {
	}

	/** NORMAL은 관측 대상에서 제외된다 — WELL/POOR만 관측 대상. */
	public static boolean isObservable(SleepLevel sleepLevel) {
		return sleepLevel == SleepLevel.WELL || sleepLevel == SleepLevel.POOR;
	}

	/** 호출 전 {@link #isObservable}로 NORMAL이 아님을 보장해야 한다. */
	public static boolean matches(SleepLevel sleepLevel, CheckInStatus checkInStatus) {
		boolean good = checkInStatus == CheckInStatus.IMPROVED;
		return sleepLevel == SleepLevel.WELL ? good : !good;
	}
}
