package com.afterglow.tracking.daily.domain;

import java.time.LocalDate;

import com.afterglow.account.domain.Account;
import com.afterglow.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

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
		Account account,
		LocalDate recordedDate,
		SleepLevel sleepLevel,
		ConditionLevel conditionLevel,
		Double temperature,
		Double humidity,
		Double uvIndex
	) {
		this.account = account;
		this.recordedDate = recordedDate;
		this.sleepLevel = sleepLevel;
		this.conditionLevel = conditionLevel;
		this.temperature = temperature;
		this.humidity = humidity;
		this.uvIndex = uvIndex;
	}

	public static DailyTracking create(
		Account account,
		LocalDate recordedDate,
		SleepLevel sleepLevel,
		ConditionLevel conditionLevel,
		Double temperature,
		Double humidity,
		Double uvIndex
	) {
		return new DailyTracking(
			account,
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
