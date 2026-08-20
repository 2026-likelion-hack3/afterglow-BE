package com.afterglow.domain.story.domain;

import java.util.List;
import java.util.Optional;

public interface StoryRepository {

	Story save(Story story);

	Optional<Story> findById(Long id);

	List<Story> findAll();

	List<Story> findByAccountId(Long accountId);

	/** 이야기 목록 — 최신순(생성일 내림차순, 동시각이면 id 내림차순으로 tie-break). */
	List<Story> findAllByOrderByCreatedAtDescIdDesc();
}
