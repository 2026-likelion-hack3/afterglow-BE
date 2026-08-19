package com.afterglow.domain.episode.routine.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RoutineCreateRequest(
		@NotNull @Valid List<RoutineItemRequest> items
) {
}
