package com.afterglow.tracking.daily.domain;

import java.time.LocalDate;

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
@Table(
	name = "daily_tracking",
	uniqueConstraints = {
		@jakarta.persistence.UniqueConstraint(
			name = "uk_daily_tracking_account_date",
			columnNames = {"account_id", "recorded_date"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyTracking extends BaseEntity {

	@Column(nullable = false)
	private Long accountId;

	@Column(nullable = false)
	private LocalDate recordedDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SleepLevel sleepLevel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ConditionLevel conditionLevel;

	private Double temperature;

	private Double humidity;

	private Double uvIndex;

	private DailyTracking(
		Long accountId,
		LocalDate recordedDate,
		SleepLevel sleepLevel,
		ConditionLevel conditionLevel,
		Double temperature,
		Double humidity,
		Double uvIndex
	) {
		this.accountId = accountId;
		this.recordedDate = recordedDate;
		this.sleepLevel = sleepLevel;
		this.conditionLevel = conditionLevel;
		this.temperature = temperature;
		this.humidity = humidity;
		this.uvIndex = uvIndex;
	}

	public static DailyTracking create(
		Long accountId,
		LocalDate recordedDate,
		SleepLevel sleepLevel,
		ConditionLevel conditionLevel,
		Double temperature,
		Double humidity,
		Double uvIndex
	) {
		return new DailyTracking(
			accountId,
			recordedDate,
			sleepLevel,
			conditionLevel,
			temperature,
			humidity,
			uvIndex
		);
	}

	public void update(
		SleepLevel sleepLevel,
		ConditionLevel conditionLevel,
		Double temperature,
		Double humidity,
		Double uvIndex
	) {
		this.sleepLevel = sleepLevel;
		this.conditionLevel = conditionLevel;
		this.temperature = temperature;
		this.humidity = humidity;
		this.uvIndex = uvIndex;
	}
}
