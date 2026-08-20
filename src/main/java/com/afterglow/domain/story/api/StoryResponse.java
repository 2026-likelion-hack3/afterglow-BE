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
	LocalDateTime createdAt
) {

	public static StoryResponse from(Story story) {
		return new StoryResponse(
			story.getId(),
			story.getTitle(),
			story.getContent(),
			story.getSymptomTags(),
			story.getSituationTags(),
			story.isLifeStagePublic() ? story.getLifeStage() : null,
			story.getLikeCount(),
			story.getCreatedAt()
		);
	}
}
