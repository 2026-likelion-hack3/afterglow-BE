package com.afterglow.domain.story.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StoryLikeRepository {

	StoryLike save(StoryLike storyLike);

	void delete(StoryLike storyLike);

	Optional<StoryLike> findByAccountIdAndStoryId(Long accountId, Long storyId);

	/** 목록 응답의 likedByMe 계산용 — story마다 따로 조회하지 않고 한 번에 가져와 N+1을 피한다. */
	List<StoryLike> findByAccountIdAndStoryIdIn(Long accountId, Collection<Long> storyIds);
}
