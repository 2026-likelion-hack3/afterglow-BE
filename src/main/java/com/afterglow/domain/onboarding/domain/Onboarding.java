package com.afterglow.domain.onboarding.domain;

import java.time.LocalDateTime;

import com.afterglow.global.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 기능명세서 1.1 — 연령대/월경 상태만 다루는 가벼운 온보딩. 계정당 하나이며 account가 삭제되면 함께 삭제된다. */
@Getter
@Entity
@Table(name = "onboarding")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Onboarding extends BaseEntity {

	@Column(nullable = false)
	private Long accountId;

	@Enumerated(EnumType.STRING)
	private AgeRange ageRange;

	@Enumerated(EnumType.STRING)
	private MenstrualStatus menstrualStatus;

	private LocalDateTime onboardingCompletedAt;

	private Onboarding(Long accountId) {
		this.accountId = accountId;
	}

	public static Onboarding createFor(Long accountId) {
		return new Onboarding(accountId);
	}

	/**
	 * 답변은 호출할 때마다 그대로 덮어쓴다(완료 후에도 수정 가능). onboardingCompletedAt은 아직 값이 없을 때만
	 * 채우고 이후 재호출로는 값이 바뀌지 않는다 — "한 번이라도 온보딩을 마쳤는지"를 나타내는 시각이지 마지막
	 * 수정 시각이 아니다. 전 항목을 건너뛰어도(둘 다 null) 완료로 취급한다.
	 */
	public void submitAnswers(AgeRange ageRange, MenstrualStatus menstrualStatus, LocalDateTime now) {
		this.ageRange = ageRange;
		this.menstrualStatus = menstrualStatus;
		if (this.onboardingCompletedAt == null) {
			this.onboardingCompletedAt = now;
		}
	}
}
