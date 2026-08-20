package com.afterglow.domain.story.infrastructure;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.afterglow.domain.story.domain.StoryLike;
import com.afterglow.domain.story.domain.StoryLikeRepository;

@Repository
public interface StoryLikeJpaRepository
	extends JpaRepository<StoryLike, Long>, StoryLikeRepository {

	List<StoryLike> findByAccountIdAndStoryIdIn(Long accountId, Collection<Long> storyIds);
}
