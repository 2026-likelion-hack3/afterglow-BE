package com.afterglow.domain.episode.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import com.afterglow.global.BaseEntity;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 증상 접수부터 3일차 판정까지 하나의 흐름을 표현하는 에피소드 — 기능명세서 2~4장. 지금은 2.1~2.2(문진 완료까지)만 다룬다. */
@Getter
@Entity
@Table(name = "episode")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Episode extends BaseEntity {

	@Column(nullable = false)
	private Long accountId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private EpisodeStatus status;

	@Embedded
	private Symptom symptom;

	@Embedded
	private Intake intake;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "episode_body_part", joinColumns = @JoinColumn(name = "episode_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "body_part")
	private Set<BodyPart> bodyParts = new LinkedHashSet<>();

	@Builder
	private Episode(Long accountId, Symptom symptom) {
		this.accountId = accountId;
		this.symptom = symptom;
		this.status = EpisodeStatus.SYMPTOM_SELECTED;
	}

	public static Episode create(Long accountId, Symptom symptom) {
		return Episode.builder()
				.accountId(accountId)
				.symptom(symptom)
				.build();
	}

	/** 2.2 고정 문진 제출. 증상 선택 직후(SYMPTOM_SELECTED) 상태에서만 허용한다. */
	public void submitIntake(Intake intake, Set<BodyPart> bodyParts) {
		if (status != EpisodeStatus.SYMPTOM_SELECTED) {
			throw new AfterglowException(ErrorCode.INVALID_EPISODE_STATE);
		}
		validateBodyParts(bodyParts);
		this.intake = intake;
		this.bodyParts = bodyParts;
		this.status = EpisodeStatus.INTAKE_COMPLETED;
	}

	private static void validateBodyParts(Set<BodyPart> bodyParts) {
		if (bodyParts.contains(BodyPart.WHOLE_FACE) && bodyParts.size() > 1) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "전체 부위는 다른 부위와 함께 선택할 수 없습니다.");
		}
	}

	/** 2.4 통합 분석 완료. 문진 완료(INTAKE_COMPLETED) 상태에서만 허용한다 — 이미 ANALYZED면 재실행하지 않는다(idempotent). */
	public void completeAnalysis() {
		if (status != EpisodeStatus.INTAKE_COMPLETED) {
			throw new AfterglowException(ErrorCode.INVALID_EPISODE_STATE);
		}
		this.status = EpisodeStatus.ANALYZED;
	}
}
