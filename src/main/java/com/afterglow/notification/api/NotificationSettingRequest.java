package com.afterglow.notification.api;

import java.time.LocalTime;

import com.afterglow.notification.domain.NotificationType;

import jakarta.validation.constraints.NotNull;

public record NotificationSettingRequest(
	@NotNull NotificationType type,
	@NotNull Boolean enabled,
	@NotNull LocalTime notificationTime,
	@NotNull String timezone
) {
}
