package com.afterglow.domain.notification.application;

import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.notification.domain.NotificationSetting;
import com.afterglow.domain.notification.domain.NotificationSettingRepository;
import com.afterglow.domain.notification.domain.NotificationType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

	private final NotificationSettingRepository notificationSettingRepository;

	public void saveOrUpdate(
		Long accountId,
		NotificationType type,
		boolean enabled,
		LocalTime notificationTime,
		String timezone) {

		NotificationSetting setting = notificationSettingRepository
			.findByAccountIdAndType(accountId, type)
			.orElse(null);

		if (setting == null) {
			setting = NotificationSetting.create(
				accountId,
				type,
				enabled,
				notificationTime,
				timezone
			);

			notificationSettingRepository.save(setting);
			return;
		}

		setting.update(
			enabled,
			notificationTime,
			timezone
		);
	}

	@Transactional(readOnly = true)
	public List<NotificationSetting> getSettings(Long accountId) {
		return notificationSettingRepository.findByAccountId(accountId);
	}
}
