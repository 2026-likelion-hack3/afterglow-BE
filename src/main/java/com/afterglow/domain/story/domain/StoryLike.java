package com.afterglow.domain.story.domain;

import com.afterglow.global.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * account가 story에 남긴 공감 1건 — account/story 쌍당 최대 1개(DB unique 제약, V13 마이그레이션).
 * 존재 여부 자체가 "공감했다"는 뜻이라 상태 필드가 없다 — 취소는 row 삭제로 표현한다.
 */
@Getter
@Entity
@Table(name = "story_like", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "story_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryLike extends BaseEntity {

	@Column(nullable = false)
	private Long accountId;

	@Column(nullable = false)
	private Long storyId;

	private StoryLike(Long accountId, Long storyId) {
		this.accountId = accountId;
		this.storyId = storyId;
	}

	public static StoryLike create(Long accountId, Long storyId) {
		return new StoryLike(accountId, storyId);
	}
}
