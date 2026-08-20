package com.afterglow.domain.episode.checkin.api;

import com.afterglow.domain.episode.checkin.domain.CheckInStatus;

import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
		@NotNull CheckInStatus status
) {
}
