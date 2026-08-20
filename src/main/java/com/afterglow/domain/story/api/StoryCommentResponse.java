package com.afterglow.domain.story.api;

import java.time.LocalDateTime;

import com.afterglow.domain.story.domain.StoryComment;

/**
 * 작성자 표시 필드는 의도적으로 없다(2026-08-20) — Figma는 lifeStage 스타일 라벨("폐경 3년차" 등)을
 * 보여주지만, 그 계산에 필요한 데이터가 아직 없어(Story lifeStage와 동일한 미해결 문제, 기획 답변 대기)
 * 이번 범위에서는 넣지 않았다. {@code StoryComment} 참고.
 */
public record StoryCommentResponse(Long commentId, String content, LocalDateTime createdAt) {

	public static StoryCommentResponse from(StoryComment comment) {
		return new StoryCommentResponse(comment.getId(), comment.getContent(), comment.getCreatedAt());
	}
}
