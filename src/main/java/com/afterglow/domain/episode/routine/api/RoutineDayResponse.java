package com.afterglow.domain.episode.routine.api;

import java.time.LocalDate;
import java.util.List;

public record RoutineDayResponse(int dayNumber, LocalDate date, List<RoutineItemResponse> items) {
}
