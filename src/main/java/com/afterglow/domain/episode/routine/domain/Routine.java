package com.afterglow.domain.episode.routine.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.afterglow.global.BaseEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 3일 루틴 — 기능명세서 3.2(Manyfast F-VUQBTM). Episode 하위 서브도메인 고유 데이터라 {@code episodeId}/
 * {@code accountId} 값 참조만 쓴다(FK 없음, checkin과 동일한 관례). Episode당 Routine은 하나만 존재한다
 * ({@code EpisodeAnalysisResult}와 같은 설계 원칙 — episode_id unique).
 *
 * <p>{@link #DURATION_DAYS}는 3일 고정이다 — Manyfast의 "루틴 기간을 조정 가능하게 할까" 질문은 아직
 * 미확정이지만, 이 기능·문서 전체가 일관되게 "3일"을 전제하고 권장안도 "3일 고정"이라 이를 기준으로
 * 구현했다(docs/domains/episode.md Pending Decisions 참고).
 */
@Getter
@Entity
@Table(name = "routine", uniqueConstraints = @UniqueConstraint(name = "uk_routine_episode", columnNames = "episode_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Routine extends BaseEntity {

	public static final int DURATION_DAYS = 3;

	@Column(nullable = false)
	private Long episodeId;

	@Column(nullable = false)
	private Long accountId;

	@Column(nullable = false)
	private LocalDate startDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RoutineStatus status;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "routine_item", joinColumns = @JoinColumn(name = "routine_id"))
	@OrderColumn(name = "item_order")
	private List<RoutineItem> items = new ArrayList<>();

	private Routine(Long episodeId, Long accountId, LocalDate startDate, List<RoutineItem> items) {
		this.episodeId = episodeId;
		this.accountId = accountId;
		this.startDate = startDate;
		this.status = RoutineStatus.ACTIVE;
		this.items = items;
	}

	public static Routine create(Long episodeId, Long accountId, LocalDate startDate, List<RoutineItem> items) {
		return new Routine(episodeId, accountId, startDate, items);
	}

	/** dayNumber(1~{@link #DURATION_DAYS})에 해당하는 실제 날짜. startDate를 기준으로 매번 계산하므로 재조회해도 항상 동일하다. */
	public LocalDate dateOf(int dayNumber) {
		validateDayNumber(dayNumber);
		return startDate.plusDays(dayNumber - 1L);
	}

	/** 특정 day의 CONTINUE 항목만(아침/저녁), 저장 순서 그대로. */
	public List<RoutineItem> continueItemsOf(int dayNumber) {
		validateDayNumber(dayNumber);
		return items.stream()
				.filter(item -> item.getUsage() == RoutineItemUsage.CONTINUE && Objects.equals(item.getDayNumber(), dayNumber))
				.toList();
	}

	/** 이 루틴 기간 동안 중단 대상으로 표시된 제품 id 목록. */
	public List<Long> discontinuedProductIds() {
		return items.stream()
				.filter(item -> item.getUsage() == RoutineItemUsage.DISCONTINUE)
				.map(RoutineItem::getProductId)
				.toList();
	}

	private static void validateDayNumber(int dayNumber) {
		if (dayNumber < 1 || dayNumber > DURATION_DAYS) {
			throw new IllegalArgumentException("dayNumber는 1~" + DURATION_DAYS + " 사이여야 합니다: " + dayNumber);
		}
	}
}
