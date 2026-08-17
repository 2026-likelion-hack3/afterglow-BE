package com.afterglow.notification.domain;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository {

	NotificationSetting save(NotificationSetting setting);

	Optional<NotificationSetting> findByAccountIdAndType(
		Long accountId,
		NotificationType type
	);

	List<NotificationSetting> findByAccountId(Long accountId);

	void delete(NotificationSetting setting);
}
