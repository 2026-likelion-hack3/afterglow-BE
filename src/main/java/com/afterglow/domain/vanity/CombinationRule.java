package com.afterglow.domain.vanity;

import com.afterglow.global.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 같은 시간대 조합을 검사하는 규칙. 운영자가 관리한다 — 기능명세서 5.2.
 * tagB가 비어 있으면 tagA 하나가 minCount 이상 같은 시간대에 몰릴 때 경고하는 규칙(예: 고농도 2개 이상)이고,
 * tagB가 있으면 두 태그가 같은 시간대에 함께 있을 때 경고하는 규칙(예: 레티놀×산)이다.
 */
@Getter
@Entity
@Table(name = "combination_rule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CombinationRule extends BaseEntity {

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private InteractionTag tagA;

	@Enumerated(EnumType.STRING)
	private InteractionTag tagB;

	@Column(nullable = false)
	private int minCount;

	@Column(nullable = false)
	private String warningMessage;

	@Builder
	private CombinationRule(InteractionTag tagA, InteractionTag tagB, int minCount, String warningMessage) {
		this.tagA = tagA;
		this.tagB = tagB;
		this.minCount = minCount == 0 ? 2 : minCount;
		this.warningMessage = warningMessage;
	}

	public boolean isSameTagThresholdRule() {
		return tagB == null;
	}
}
