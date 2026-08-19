package com.afterglow.domain.episode.routine.api;

import java.time.LocalDate;
import java.util.List;

import com.afterglow.domain.episode.routine.domain.RoutineStatus;

public record RoutineResponse(
		Long id,
		LocalDate startDate,
		RoutineStatus status,
		List<RoutineDayResponse> days,
		List<Long> discontinuedProductIds
) {
}
