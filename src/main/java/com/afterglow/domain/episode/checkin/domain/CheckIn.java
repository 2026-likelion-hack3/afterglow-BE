package com.afterglow.domain.episode.checkin.domain;

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

/**
 * 하루 한 번, 처음 시작했을 때와 비교한 상태를 기록한다 — 기능명세서 4.1. Episode 하위 서브도메인 고유
 * 데이터라 다른 서브도메인과 공유하지 않고 checkin 패키지 안에 둔다(Episode/Symptom/Intake와 달리
 * domain.episode.domain 공유 위치가 아님).
 */
@Getter
@Entity
@Table(name = "check_in")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckIn extends BaseEntity {

	@Column(nullable = false)
	private Long episodeId;

	@Column(nullable = false)
	private LocalDate checkInDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CheckInStatus status;

	private CheckIn(Long episodeId, LocalDate checkInDate, CheckInStatus status) {
		this.episodeId = episodeId;
		this.checkInDate = checkInDate;
		this.status = status;
	}

	public static CheckIn create(Long episodeId, LocalDate checkInDate, CheckInStatus status) {
		return new CheckIn(episodeId, checkInDate, status);
	}

	/** 하루에 여러 번 기록하면 마지막 응답으로 덮어쓴다(Manyfast F-SQUDJA exceptions 확정 사항). */
	public void overwrite(CheckInStatus status) {
		this.status = status;
	}
}
