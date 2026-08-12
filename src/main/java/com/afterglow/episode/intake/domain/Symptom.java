package com.afterglow.episode.intake.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 증상 극좌표 격자 입력 — 기능명세서 2.1. angle/radius는 UI가 준 raw 값을, primarySymptom/severity는
 * FE가 확정해 전달한 값을 그대로 저장한다. 서버는 angle→primarySymptom, radius→severity 변환을 하지 않는다
 * (정확한 경계값이 명세에 없고, 이 변환은 통합 분석 범위에 속함).
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Symptom {

	@Column(name = "symptom_angle", nullable = false)
	private double angle;

	@Column(name = "symptom_radius", nullable = false)
	private double radius;

	@Column(name = "primary_symptom", nullable = false)
	@Enumerated(EnumType.STRING)
	private PrimarySymptom primarySymptom;

	@Column(name = "severity", nullable = false)
	@Enumerated(EnumType.STRING)
	private Severity severity;

	@Builder
	private Symptom(double angle, double radius, PrimarySymptom primarySymptom, Severity severity) {
		this.angle = angle;
		this.radius = radius;
		this.primarySymptom = primarySymptom;
		this.severity = severity;
	}

	public static Symptom create(double angle, double radius, PrimarySymptom primarySymptom, Severity severity) {
		return Symptom.builder()
				.angle(angle)
				.radius(radius)
				.primarySymptom(primarySymptom)
				.severity(severity)
				.build();
	}
}
