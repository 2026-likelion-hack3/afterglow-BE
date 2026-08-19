package com.afterglow.domain.story.application;

import java.util.Collections;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.story.domain.LifeStage;
import com.afterglow.domain.story.domain.SituationTag;
import com.afterglow.domain.story.domain.Story;
import com.afterglow.domain.story.domain.StoryRepository;
import com.afterglow.domain.story.domain.SymptomTag;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StoryService {

	private final StoryRepository storyRepository;

	public Long createStory(
		Long accountId,
		String title,
		String content,
		Set<SymptomTag> symptomTags,
		Set<SituationTag> situationTags,
		LifeStage lifeStage,
		boolean lifeStagePublic) {

		if (symptomTags == null || symptomTags.isEmpty()) {
			throw new AfterglowException(
				ErrorCode.INVALID_REQUEST,
				"증상 태그를 하나 이상 선택해야 합니다."
			);
		}

		Set<SituationTag> safeSituationTags =
			situationTags == null
				? Collections.emptySet()
				: situationTags;

		Story story = Story.create(
			accountId,
			title,
			content,
			symptomTags,
			safeSituationTags,
			lifeStage,
			lifeStagePublic
		);

		storyRepository.save(story);

		return story.getId();
	}
}
