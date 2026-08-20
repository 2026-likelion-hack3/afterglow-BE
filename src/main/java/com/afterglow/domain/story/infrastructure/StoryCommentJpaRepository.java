package com.afterglow.domain.story.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.afterglow.domain.story.domain.StoryComment;
import com.afterglow.domain.story.domain.StoryCommentRepository;

@Repository
public interface StoryCommentJpaRepository
	extends JpaRepository<StoryComment, Long>, StoryCommentRepository {
}
