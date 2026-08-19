package com.afterglow.domain.episode.routine.api;

import com.afterglow.domain.episode.routine.domain.RoutineTimeSlot;

public record RoutineItemResponse(RoutineTimeSlot timeSlot, Long productId) {
}
