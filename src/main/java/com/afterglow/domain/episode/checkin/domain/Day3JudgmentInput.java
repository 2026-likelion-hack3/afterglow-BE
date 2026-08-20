package com.afterglow.domain.episode.checkin.domain;

/**
 * 3일 관찰 구간의 각 날짜(시작일 기준 Day1/Day2/Day3)에 대한 응답. 값이 없으면(건너뛴 날) null이다.
 *
 * <p>이 입력은 이미 "어떤 CheckIn이 Day1/Day2/Day3에 해당하는지"가 정해진 상태를 전제한다 — 그 매핑은
 * Routine의 시작일이 있어야 할 수 있는데 아직 `routine`이 구현되지 않아 이 매핑을 수행할 방법이 없다.
 * 그래서 이 판정 엔진은 그 매핑이 이미 끝난 값을 입력으로 받기만 하고, "저장된 CheckIn 중 어느 것이
 * 몇 일차인지 추측"하는 일은 하지 않는다 — 그 orchestration은 routine 구현 이후 별도로 다룬다.
 */
public record Day3JudgmentInput(
		CheckInStatus day1Status,
		CheckInStatus day2Status,
		CheckInStatus day3Status
) {
}
