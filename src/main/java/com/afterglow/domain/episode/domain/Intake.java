package com.afterglow.domain.episode.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고정 문진 — 기능명세서 2.2. recentNewProductName은 화장대 연동 전까지의 정식 직접입력 경로다
 * (vanity 연동 시 대체될 임시 필드가 아니라, 추후 optional recentNewProductId를 별도로 추가하는 방식으로 확장한다).
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Intake {

	/** Episode 생성 시점엔 intake 전체가 null이므로 컬럼 레벨 nullable=false를 걸면 안 된다 — 필수 여부는 IntakeRequest의 @NotNull로 검증한다. */
	@Column(name = "onset_period")
	@Enumerated(EnumType.STRING)
	private OnsetPeriod onsetPeriod;

	private String recentNewProductName;

	private String notes;

	@Builder
	private Intake(OnsetPeriod onsetPeriod, String recentNewProductName, String notes) {
		this.onsetPeriod = onsetPeriod;
		this.recentNewProductName = recentNewProductName;
		this.notes = notes;
	}

	public static Intake create(OnsetPeriod onsetPeriod, String recentNewProductName, String notes) {
		return Intake.builder()
				.onsetPeriod(onsetPeriod)
				.recentNewProductName(recentNewProductName)
				.notes(notes)
				.build();
	}
}
