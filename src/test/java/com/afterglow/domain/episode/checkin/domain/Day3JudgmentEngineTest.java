package com.afterglow.domain.episode.checkin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Day3JudgmentEngineTest {

	private final Day3JudgmentEngine engine = new Day3JudgmentEngine();

	@Test
	void 삼일차가_IMPROVED이면_MAINTAIN이다() {
		Day3JudgmentResult result = engine.judge(
				new Day3JudgmentInput(CheckInStatus.SAME, CheckInStatus.SAME, CheckInStatus.IMPROVED));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.MAINTAIN);
	}

	@Test
	void 삼일차가_SAME이면_EXTEND이다() {
		Day3JudgmentResult result = engine.judge(
				new Day3JudgmentInput(CheckInStatus.IMPROVED, CheckInStatus.IMPROVED, CheckInStatus.SAME));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.EXTEND);
	}

	@Test
	void 삼일차가_WORSE이면_STOP이다() {
		Day3JudgmentResult result = engine.judge(
				new Day3JudgmentInput(CheckInStatus.IMPROVED, CheckInStatus.IMPROVED, CheckInStatus.WORSE));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.STOP);
	}

	@Test
	void 일일차가_WORSE이면_삼일을_기다리지_않고_즉시_STOP이다() {
		Day3JudgmentResult result = engine.judge(
				new Day3JudgmentInput(CheckInStatus.WORSE, CheckInStatus.IMPROVED, CheckInStatus.IMPROVED));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.STOP);
	}

	@Test
	void 이일차가_WORSE이면_즉시_STOP이다() {
		Day3JudgmentResult result = engine.judge(
				new Day3JudgmentInput(CheckInStatus.SAME, CheckInStatus.WORSE, CheckInStatus.IMPROVED));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.STOP);
	}

	@Test
	void 일일차만_WORSE이고_나머지가_미응답이어도_보류보다_즉시_STOP이_우선한다() {
		Day3JudgmentResult result = engine.judge(
				new Day3JudgmentInput(CheckInStatus.WORSE, null, null));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.STOP);
	}

	@Test
	void 세_날_모두_미응답이면_WITHHELD이다() {
		Day3JudgmentResult result = engine.judge(new Day3JudgmentInput(null, null, null));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.WITHHELD);
	}

	@Test
	void 응답이_한_날뿐이면_WITHHELD이다() {
		Day3JudgmentResult result = engine.judge(
				new Day3JudgmentInput(CheckInStatus.IMPROVED, null, null));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.WITHHELD);
	}

	@Test
	void 응답이_두_날이면_WITHHELD이다() {
		Day3JudgmentResult result = engine.judge(
				new Day3JudgmentInput(CheckInStatus.IMPROVED, null, CheckInStatus.SAME));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.WITHHELD);
	}

	@Test
	void 이일차를_건너뛰어도_일일차와_삼일차만으로는_이일_이하라_WITHHELD이다() {
		// 8/1 응답, 8/2 미응답(건너뜀), 8/3 응답 — "날짜 사이 공백"이 아니라 "응답이 2일뿐"이라는
		// 이유로 WITHHELD가 되는지를 확인한다(#37 exceptions: 건너뛴 날은 빈 값).
		Day3JudgmentResult result = engine.judge(
				new Day3JudgmentInput(CheckInStatus.IMPROVED, null, CheckInStatus.IMPROVED));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.WITHHELD);
	}

	@Test
	void 삼일_모두_응답이고_WORSE가_없으면_삼일차_기준으로_판정한다() {
		Day3JudgmentResult result = engine.judge(
				new Day3JudgmentInput(CheckInStatus.SAME, CheckInStatus.SAME, CheckInStatus.IMPROVED));

		assertThat(result.verdict()).isEqualTo(Day3Verdict.MAINTAIN);
	}
}
