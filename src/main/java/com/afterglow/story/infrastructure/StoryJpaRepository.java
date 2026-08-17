package com.afterglow.story.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.afterglow.story.domain.Story;
import com.afterglow.story.domain.StoryRepository;

@Repository
public interface StoryJpaRepository
	extends JpaRepository<Story, Long>, StoryRepository {

	List<Story> findByAccountId(Long accountId);
}
