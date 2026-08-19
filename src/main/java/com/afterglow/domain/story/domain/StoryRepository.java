package com.afterglow.domain.story.domain;

import java.util.List;
import java.util.Optional;

public interface StoryRepository {

	Story save(Story story);

	Optional<Story> findById(Long id);

	List<Story> findAll();

	List<Story> findByAccountId(Long accountId);
}
