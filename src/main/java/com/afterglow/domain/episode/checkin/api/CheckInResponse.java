package com.afterglow.domain.episode.checkin.api;

import java.time.LocalDate;

import com.afterglow.domain.episode.checkin.domain.CheckInStatus;

public record CheckInResponse(LocalDate checkInDate, CheckInStatus status) {
}
