package com.afterglow.domain.story.api;

import java.util.Set;

import com.afterglow.domain.story.domain.LifeStage;
import com.afterglow.domain.story.domain.SituationTag;
import com.afterglow.domain.story.domain.SymptomTag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record StoryCreateRequest(
	@NotBlank String title,

	@NotBlank String content,

	@NotEmpty Set<SymptomTag> symptomTags,

	Set<SituationTag> situationTags,

	LifeStage lifeStage,

	@NotNull Boolean lifeStagePublic
) {
}
