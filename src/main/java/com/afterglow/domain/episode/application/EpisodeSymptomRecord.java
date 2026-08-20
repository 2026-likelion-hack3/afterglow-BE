package com.afterglow.domain.episode.application;

import java.time.LocalDateTime;

import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;

/** 다른 도메인(Tracking 등)이 기간별 증상 조회에 필요한 최소 필드만 담는 read model. */
public record EpisodeSymptomRecord(
		Long episodeId,
		LocalDateTime createdAt,
		PrimarySymptom primarySymptom,
		Severity severity) {
}
