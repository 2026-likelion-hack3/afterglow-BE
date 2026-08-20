package com.afterglow.domain.notification.domain;

import java.time.LocalTime;

import com.afterglow.global.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notification_setting")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseEntity {

	@Column(nullable = false)
	private Long accountId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationType type;

	@Column(nullable = false)
	private boolean enabled;

	@Column(nullable = false)
	private LocalTime notificationTime;

	@Column(nullable = false)
	private String timezone;

	private NotificationSetting(
		Long accountId,
		NotificationType type,
		boolean enabled,
		LocalTime notificationTime,
		String timezone) {
		this.accountId = accountId;
		this.type = type;
		this.enabled = enabled;
		this.notificationTime = notificationTime;
		this.timezone = timezone;
	}

	public static NotificationSetting create(
		Long accountId,
		NotificationType type,
		boolean enabled,
		LocalTime notificationTime,
		String timezone) {

		return new NotificationSetting(
			accountId,
			type,
			enabled,
			notificationTime,
			timezone
		);
	}

	public void update(
		boolean enabled,
		LocalTime notificationTime,
		String timezone) {

		this.enabled = enabled;
		this.notificationTime = notificationTime;
		this.timezone = timezone;
	}
}
