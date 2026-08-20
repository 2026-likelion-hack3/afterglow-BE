package com.afterglow.domain.vanity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.afterglow.domain.vanity.InteractionTag;

class InteractionTagMatcherTest {

	@Test
	void 레티놀_키워드가_있으면_RETINOL을_매칭한다() {
		assertThat(InteractionTagMatcher.match("정제수, 레티놀, 다이메티콘"))
				.containsExactly(InteractionTag.RETINOL);
	}

	@Test
	void 영문_INCI명으로도_매칭한다() {
		assertThat(InteractionTagMatcher.match("Water, Retinol, Dimethicone"))
				.containsExactly(InteractionTag.RETINOL);
	}

	@Test
	void 살리실릭애씨드는_ACID로_매칭한다() {
		assertThat(InteractionTagMatcher.match("정제수, 살리실릭애씨드"))
				.containsExactly(InteractionTag.ACID);
	}

	@Test
	void 아스코르빅애씨드는_VITAMIN_C로_매칭한다() {
		assertThat(InteractionTagMatcher.match("정제수, 아스코르빅애씨드"))
				.containsExactly(InteractionTag.VITAMIN_C);
	}

	@Test
	void 세_태그_키워드가_모두_있으면_모두_매칭한다() {
		assertThat(InteractionTagMatcher.match("레티놀, 글라이콜릭애씨드, 비타민씨"))
				.containsExactlyInAnyOrder(InteractionTag.RETINOL, InteractionTag.ACID, InteractionTag.VITAMIN_C);
	}

	@Test
	void 관련_키워드가_없으면_빈_Set이다() {
		assertThat(InteractionTagMatcher.match("정제수, 글리세린, 나이아신아마이드")).isEmpty();
	}

	@Test
	void null이면_빈_Set이다() {
		assertThat(InteractionTagMatcher.match(null)).isEmpty();
	}

	@Test
	void blank면_빈_Set이다() {
		assertThat(InteractionTagMatcher.match("   ")).isEmpty();
	}

	@Test
	void 단어_경계_밖의_우연한_부분문자열은_오탐하지_않는다() {
		// "aha"가 다른 단어에 우연히 포함돼도(예: "aha다시마추출물" 같은 합성어가 아니라 실제로는 붙어
		// 있지 않은 상황을 가정) 단어 경계가 없는 매칭이면 오탐할 수 있다 — 여기서는 관련 없는 텍스트에
		// AHA라는 독립된 토큰이 없을 때 매칭되지 않는지 확인한다.
		assertThat(InteractionTagMatcher.match("정제수, 하이알루로닉애씨드")).isEmpty();
	}

	@Test
	void 대소문자를_구분하지_않는다() {
		assertThat(InteractionTagMatcher.match("water, RETINOL"))
				.containsExactly(InteractionTag.RETINOL);
	}

	@Test
	void HIGH_CONCENTRATION과_LOW_IRRITATION_키워드는_정의돼_있지_않다() {
		assertThat(InteractionTagMatcher.match("고농도, 저자극, 고농축, 저자극테스트")).isEmpty();
	}
}
