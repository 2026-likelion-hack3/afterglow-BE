package com.afterglow.domain.story.domain;

import java.util.List;

public interface StoryCommentRepository {

	StoryComment save(StoryComment storyComment);

	/**
	 * 정렬 규칙(2026-08-20, Figma G2:글상세 node 967:277 확인) — 오래된 순(작성일 오름차순, 동시각이면
	 * id 오름차순 tie-break). Story 목록의 최신순과는 반대다 — 임의로 확장한 게 아니라 Figma 와이어프레임의
	 * 댓글 3개 표시 순서(2일 전 → 1일 전 → 어제)를 그대로 따른 것이다.
	 */
	List<StoryComment> findByStoryIdOrderByCreatedAtAscIdAsc(Long storyId);
}
