package com.afterglow.domain.story.api;

/** 공감 토글 결과 — 토글 이후의 최종 상태(liked=true면 방금 눌러서 추가됨, false면 취소됨). */
public record StoryLikeResponse(boolean liked, int likeCount) {
}
