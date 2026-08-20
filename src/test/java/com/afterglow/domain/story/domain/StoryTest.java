package com.afterglow.domain.story.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class StoryTest {

	private Story newStory() {
		return Story.create(1L, "제목", "본문", Set.of(SymptomTag.REDNESS), Set.of(), null, false);
	}

	@Test
	void increaseLikeCount를_호출하면_likeCount가_1_증가한다() {
		Story story = newStory();

		story.increaseLikeCount();

		assertThat(story.getLikeCount()).isEqualTo(1);
	}

	@Test
	void decreaseLikeCount는_0에서_호출해도_0_미만으로_내려가지_않는다() {
		Story story = newStory();

		story.decreaseLikeCount();

		assertThat(story.getLikeCount()).isEqualTo(0);
	}

	@Test
	void increase_두번_후_decrease_한번이면_1이_남는다() {
		Story story = newStory();

		story.increaseLikeCount();
		story.increaseLikeCount();
		story.decreaseLikeCount();

		assertThat(story.getLikeCount()).isEqualTo(1);
	}
}
