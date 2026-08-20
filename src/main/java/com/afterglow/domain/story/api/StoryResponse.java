package com.afterglow.domain.story.api;

import java.time.LocalDateTime;
import java.util.Set;

import com.afterglow.domain.story.domain.LifeStage;
import com.afterglow.domain.story.domain.SituationTag;
import com.afterglow.domain.story.domain.Story;
import com.afterglow.domain.story.domain.SymptomTag;

public record StoryResponse(
	Long id,
	String title,
	String content,
	Set<SymptomTag> symptomTags,
	Set<SituationTag> situationTags,
	LifeStage lifeStage,
	int likeCount,
	boolean likedByMe,
	LocalDateTime createdAt
) {

	/**
	 * likedByMe는 현재 요청 계정 기준 공감 여부다(비로그인이면 항상 false) — {@code StoryService}가 계산해서
	 * 넘긴다. symptomTags/situationTags는 {@code Set.copyOf}로 즉시 복사한다 —
	 * {@code @ElementCollection(fetch = LAZY)}라 이 메서드 호출 시점(트랜잭션 안)에 강제로 읽어두지
	 * 않으면, 나중에 Jackson이 트랜잭션 밖에서 직렬화할 때 {@code LazyInitializationException}이 난다
	 * (2026-08-20 로컬 seed 검증 중 GET /api/stories/{id}에서 실제로 재현·확인됨 — 목록 조회는 필터
	 * 로직이 우연히 먼저 컬렉션을 건드려 가려져 있었을 뿐이었다).
	 */
	public static StoryResponse from(Story story, boolean likedByMe) {
		return new StoryResponse(
			story.getId(),
			story.getTitle(),
			story.getContent(),
			Set.copyOf(story.getSymptomTags()),
			Set.copyOf(story.getSituationTags()),
			story.isLifeStagePublic() ? story.getLifeStage() : null,
			story.getLikeCount(),
			likedByMe,
			story.getCreatedAt()
		);
	}
}
