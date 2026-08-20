package com.afterglow.domain.episode.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 다른 backend 도메인(Tracking 등)이 Episode 데이터를 조회할 때 쓰는 내부 query service. Controller는 두지 않는다.
 * SELECT 1회 + Embedded 필드(즉시 로딩) 읽기만 하고 lazy 연관관계에 접근하지 않아, Spring Data repository
 * 호출 자체가 보장하는 트랜잭션 이상의 별도 경계가 필요 없다 — 그래서 서비스 레벨 @Transactional을 두지 않는다.
 */
@Service
@RequiredArgsConstructor
public class EpisodeQueryService {

	private final EpisodeRepository episodeRepository;

	/** from~to(둘 다 포함) 기간의 증상 기록을 createdAt 오름차순으로 반환한다. */
	public List<EpisodeSymptomRecord> findSymptomsByPeriod(Long accountId, LocalDate from, LocalDate to) {
		validatePeriod(from, to);
		LocalDateTime fromInclusive = from.atStartOfDay();
		LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

		return episodeRepository
				.findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(accountId, fromInclusive, toExclusive)
				.stream()
				.map(this::toRecord)
				.toList();
	}

	private EpisodeSymptomRecord toRecord(Episode episode) {
		return new EpisodeSymptomRecord(
				episode.getId(), episode.getCreatedAt(), episode.getSymptom().getPrimarySymptom(), episode.getSymptom().getSeverity());
	}

	private void validatePeriod(LocalDate from, LocalDate to) {
		if (from == null || to == null) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "조회 기간(from, to)은 필수입니다.");
		}
		if (from.isAfter(to)) {
			throw new AfterglowException(ErrorCode.INVALID_REQUEST, "시작일은 종료일보다 늦을 수 없습니다.");
		}
	}
}
