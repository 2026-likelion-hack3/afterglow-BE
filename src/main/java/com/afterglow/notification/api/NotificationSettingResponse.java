package com.afterglow.notification.api;

import java.time.LocalTime;

import com.afterglow.notification.domain.NotificationSetting;
import com.afterglow.notification.domain.NotificationType;

public record NotificationSettingResponse(
	Long id,
	NotificationType type,
	boolean enabled,
	LocalTime notificationTime,
	String timezone
) {

	public static NotificationSettingResponse from(
		NotificationSetting setting) {

		return new NotificationSettingResponse(
			setting.getId(),
			setting.getType(),
			setting.isEnabled(),
			setting.getNotificationTime(),
			setting.getTimezone()
		);
	}
}
