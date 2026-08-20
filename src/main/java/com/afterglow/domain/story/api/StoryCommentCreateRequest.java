package com.afterglow.domain.story.api;

import jakarta.validation.constraints.NotBlank;

/** 길이 제한은 두지 않는다 — Manyfast/Figma에 댓글 길이 제한이 명시돼 있지 않아, {@code Story.content}와 동일하게 blank만 막는다. */
public record StoryCommentCreateRequest(@NotBlank String content) {
}
