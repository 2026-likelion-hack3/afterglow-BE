package com.afterglow.domain.episode.analysis.domain;

import java.time.LocalDate;
import java.util.List;

import com.afterglow.domain.episode.card.domain.ResultCard;
import com.afterglow.domain.episode.card.domain.ResultCardHoldReason;
import com.afterglow.domain.episode.card.domain.ResultCardResult;
import com.afterglow.domain.episode.card.domain.ResultCardType;
import com.afterglow.global.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@link ResultCardResult}(3.1, Manyfast F-HGUJDZ)의 영속화. Episode당 하나만 존재한다(episode_id
 * unique) — {@code EpisodeAnalysisService}가 재분석 없이 그대로 반환할 수 있게(idempotent).
 *
 * <p><b>저장 범위 축소(2026-08-19)</b>: 엔진의 원본 {@link AnalysisResult}(전체 candidates/exclusions
 * 목록)를 그대로 저장하지 않고, {@link com.afterglow.domain.episode.card.domain.ResultCardAssembler}가
 * 만든 {@link ResultCardResult}의 데이터(카드 3장 중 실제 값을 가진 첫 번째 카드 하나 + hold/holdReason/
 * confidence)만 저장한다 — 두 번째(CONTINUE_USE)/세 번째(HOSPITAL_VISIT) 카드는 항상 빈 내용이라 저장할
 * 값이 없고, "다음 원인 후보로 이동"(3일차 비슷함 분기) 기능은 이번 orchestration 범위에 없어 전체
 * candidates 순서를 보존할 필요가 아직 없다. 그 기능을 붙일 때 이 엔티티를 확장한다.
 */
@Getter
@Entity
@Table(name = "episode_analysis_result", uniqueConstraints = @UniqueConstraint(name = "uk_episode_analysis_result_episode", columnNames = "episode_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EpisodeAnalysisResult extends BaseEntity {

	@Column(nullable = false)
	private Long episodeId;

	@Column(nullable = false)
	private Long accountId;

	@Column(nullable = false)
	private boolean hold;

	@Enumerated(EnumType.STRING)
	private ResultCardHoldReason holdReason;

	@Enumerated(EnumType.STRING)
	private Confidence confidence;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ResultCardType cardType;

	@Enumerated(EnumType.STRING)
	private CandidateType causeType;

	private Long coverageDays;

	// TimingEvidence(PRODUCT)
	private Long evidenceProductId;
	private LocalDate evidenceUsageStartDate;
	private LocalDate evidenceSymptomStartDate;

	// CombinationEvidence(COMBINATION) — 단일 대문자로 끝나는 필드명은 Hibernate 기본 naming strategy가
	// evidence_taga/evidence_tagb로 계산해(evidence_tag_a가 아님) 마이그레이션과 어긋나 명시적으로 지정한다.
	@Column(name = "evidence_tag_a")
	private String evidenceTagA;
	@Column(name = "evidence_tag_b")
	private String evidenceTagB;
	@Enumerated(EnumType.STRING)
	private ConflictPlacement evidenceConflictPlacement;

	// FrequencyEvidence(SLEEP/WEATHER)
	private Integer evidenceObservationCount;
	private Integer evidenceMatchedObservationCount;

	private EpisodeAnalysisResult(Long episodeId, Long accountId, ResultCardResult result) {
		this.episodeId = episodeId;
		this.accountId = accountId;
		this.hold = result.hold();
		this.holdReason = result.holdReason();
		this.confidence = result.confidence();

		ResultCard firstCard = result.cards().get(0);
		this.cardType = firstCard.type();
		this.causeType = firstCard.causeType();
		this.coverageDays = firstCard.coverageDays();
		applyEvidence(firstCard.evidence());
	}

	public static EpisodeAnalysisResult from(Long episodeId, Long accountId, ResultCardResult result) {
		return new EpisodeAnalysisResult(episodeId, accountId, result);
	}

	private void applyEvidence(Evidence evidence) {
		switch (evidence) {
			case TimingEvidence timing -> {
				this.evidenceProductId = timing.productId();
				this.evidenceUsageStartDate = timing.usageStartDate();
				this.evidenceSymptomStartDate = timing.symptomStartDate();
			}
			case CombinationEvidence combination -> {
				this.evidenceTagA = combination.tagA();
				this.evidenceTagB = combination.tagB();
				this.evidenceConflictPlacement = combination.conflictPlacement();
			}
			case FrequencyEvidence frequency -> {
				this.evidenceObservationCount = frequency.observationCount();
				this.evidenceMatchedObservationCount = frequency.matchedObservationCount();
			}
			case null -> {
				// WITHHELD/DO_MORE_TODAY 등 근거를 참조하지 않는 카드 — evidence 컬럼 전부 null 유지.
			}
		}
	}

	/** 저장된 값으로 {@link ResultCardResult}를 다시 조립한다 — 재분석 없이 그대로 응답에 쓴다. */
	public ResultCardResult toResultCardResult() {
		ResultCard firstCard = new ResultCard(cardType, causeType, reconstructEvidence(), coverageDays);
		ResultCard continueUseCard = new ResultCard(ResultCardType.CONTINUE_USE, null, null, null);
		ResultCard hospitalCard = new ResultCard(ResultCardType.HOSPITAL_VISIT, null, null, null);
		List<ResultCard> cards = List.of(firstCard, continueUseCard, hospitalCard);

		return hold ? ResultCardResult.hold(holdReason, cards) : ResultCardResult.determined(confidence, cards);
	}

	private Evidence reconstructEvidence() {
		if (causeType == null) {
			return null;
		}
		return switch (causeType) {
			case PRODUCT -> new TimingEvidence(evidenceProductId, evidenceUsageStartDate, evidenceSymptomStartDate);
			case COMBINATION -> new CombinationEvidence(evidenceTagA, evidenceTagB, evidenceConflictPlacement);
			case SLEEP, WEATHER -> new FrequencyEvidence(evidenceObservationCount, evidenceMatchedObservationCount);
		};
	}
}
