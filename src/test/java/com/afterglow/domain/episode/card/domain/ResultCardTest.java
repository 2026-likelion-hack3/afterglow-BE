package com.afterglow.domain.episode.card.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ResultCardTest {

	@Test
	void 기존_4개_인자_생성자는_continueUseProductIds가_빈_목록이다() {
		ResultCard card = new ResultCard(ResultCardType.CONTINUE_USE, null, null, null);

		assertThat(card.continueUseProductIds()).isEmpty();
	}

	@Test
	void 신규_5개_인자_생성자에_null을_주면_빈_목록으로_방어된다() {
		ResultCard card = new ResultCard(ResultCardType.CONTINUE_USE, null, null, null, null);

		assertThat(card.continueUseProductIds()).isEmpty();
	}

	@Test
	void 신규_5개_인자_생성자로_넘긴_목록은_그대로_보존되고_불변이다() {
		ResultCard card = new ResultCard(ResultCardType.CONTINUE_USE, null, null, null, List.of(1L, 2L));

		assertThat(card.continueUseProductIds()).containsExactly(1L, 2L);
		assertThatThrownBy(() -> card.continueUseProductIds().add(3L))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void 생성자에_넘긴_원본_리스트를_수정해도_카드에는_영향이_없다() {
		List<Long> mutable = new ArrayList<>(List.of(1L, 2L));
		ResultCard card = new ResultCard(ResultCardType.CONTINUE_USE, null, null, null, mutable);

		mutable.add(3L);

		assertThat(card.continueUseProductIds()).containsExactly(1L, 2L);
	}
}
