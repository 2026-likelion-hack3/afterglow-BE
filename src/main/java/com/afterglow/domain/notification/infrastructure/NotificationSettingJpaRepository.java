package com.afterglow.domain.notification.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.afterglow.domain.notification.domain.NotificationSetting;
import com.afterglow.domain.notification.domain.NotificationSettingRepository;
import com.afterglow.domain.notification.domain.NotificationType;

@Repository
public interface NotificationSettingJpaRepository
	extends JpaRepository<NotificationSetting, Long>, NotificationSettingRepository {

	Optional<NotificationSetting> findByAccountIdAndType(
		Long accountId,
		NotificationType type
	);

	List<NotificationSetting> findByAccountId(Long accountId);
}
