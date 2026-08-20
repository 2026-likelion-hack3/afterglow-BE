package com.afterglow.domain.episode.checkin.domain;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Manyfast F-TWLPPZ("3일차 판정과 분기")/F-SQUDJA 확정 규칙을 그대로 구현한 결정적(pure) 판단 엔진.
 * Repository나 다른 Bean에 의존하지 않고, 이미 Day1/Day2/Day3로 정리된 {@link Day3JudgmentInput}만으로
 * 판정한다 — 같은 입력이면 항상 같은 결과를 반환한다.
 *
 * <p>규칙(F-TWLPPZ rules/exceptions, F-SQUDJA exceptions):
 * <ul>
 *   <li>"1일차나 2일차에 나빠졌다가 나오면 3일을 기다리지 않고 즉시 중단"이며, "판정은 3일차 응답을
 *       기준으로 한다"에는 3일차가 나빠졌을 때도 당연히 포함된다 — 즉 Day1/Day2/Day3 어디서든
 *       WORSE가 있으면 STOP.</li>
 *   <li>WORSE가 없고 응답(비어있지 않은 날)이 2일 이하면 판정하지 않고 보류(WITHHELD).</li>
 *   <li>WORSE가 없고 3일 모두 응답이면 3일차 응답 기준: 좋아졌다 → MAINTAIN, 비슷하다 → EXTEND.</li>
 * </ul>
 */
public final class Day3JudgmentEngine {

	private static final int WITHHOLD_MAX_ANSWERED_DAYS = 2;

	public Day3JudgmentResult judge(Day3JudgmentInput input) {
		if (input.day1Status() == CheckInStatus.WORSE
				|| input.day2Status() == CheckInStatus.WORSE
				|| input.day3Status() == CheckInStatus.WORSE) {
			return new Day3JudgmentResult(Day3Verdict.STOP);
		}

		long answeredDays = Stream.of(input.day1Status(), input.day2Status(), input.day3Status())
				.filter(Objects::nonNull)
				.count();
		if (answeredDays <= WITHHOLD_MAX_ANSWERED_DAYS) {
			return new Day3JudgmentResult(Day3Verdict.WITHHELD);
		}

		// 이 시점에서 3일 모두 응답이 있고 WORSE는 없으므로 day3Status는 IMPROVED 또는 SAME뿐이다.
		if (input.day3Status() == CheckInStatus.IMPROVED) {
			return new Day3JudgmentResult(Day3Verdict.MAINTAIN);
		}
		return new Day3JudgmentResult(Day3Verdict.EXTEND);
	}
}
