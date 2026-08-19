package com.afterglow.domain.episode.routine.domain;

import java.util.List;

/** Routine 생성 입력. episodeId/accountId/시작일은 호출자(API 인증/경로/서버 시각)에서 정해지므로 여기 포함하지 않는다. */
public record RoutineCreateInput(List<RoutineItemInput> items) {
}
