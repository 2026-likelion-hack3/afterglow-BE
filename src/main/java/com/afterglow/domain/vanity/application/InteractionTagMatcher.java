package com.afterglow.domain.vanity.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import com.afterglow.domain.vanity.InteractionTag;

/**
 * {@code keyIngredients} 원문에서 {@link InteractionTag}를 결정적으로 매칭한다 — AI가 아니라 성분명
 * 문자열 대조만 한다. docs/domains/vanity.md Confirmed Decisions의 "성분 동의어 사전(성분명 →
 * InteractionTag 매핑, git으로 관리)"을 최소 구현한 것이다.
 *
 * <p>{@link InteractionTag#HIGH_CONCENTRATION}/{@link InteractionTag#LOW_IRRITATION}은 성분명만으로는
 * 판단할 수 없어(농도·배합비는 라벨 텍스트에 없음) 이 매처의 대상이 아니다 — 매칭하지 않고 항상 빈
 * 값으로 남긴다. 아래 키워드 목록은 자극적이지 않고 널리 쓰이는 INCI/관용 표기만 담은 1차 curation이다
 * — 정확도를 우선해 애매하면 매칭하지 않는다(false positive보다 미검출을 택한다). 새 이명(異名)이
 * 필요하면 이 목록에 추가하는 것으로 확장한다(별도 DB/어드민 없이 git으로 관리하기로 한 결정과 동일).
 */
final class InteractionTagMatcher {

	private static final Map<InteractionTag, List<String>> KEYWORDS = Map.of(
			InteractionTag.RETINOL, List.of(
					"레티놀", "레티날", "레티닐팔미테이트",
					"retinol", "retinal", "retinyl palmitate"
			),
			InteractionTag.ACID, List.of(
					"살리실릭애씨드", "살리실산", "글라이콜릭애씨드", "글리콜산", "락틱애씨드", "젖산", "만델릭애씨드",
					"salicylic acid", "glycolic acid", "lactic acid", "mandelic acid", "AHA", "BHA"
			),
			InteractionTag.VITAMIN_C, List.of(
					"아스코빅애씨드", "아스코르빅애씨드", "아스코르빈산", "아스코르빌글루코사이드",
					"마그네슘아스코빌포스페이트", "소듐아스코빌포스페이트", "테트라헥실데실아스코르베이트", "비타민씨", "비타민 C",
					"ascorbic acid", "vitamin c"
			)
	);

	private static final Map<InteractionTag, List<Pattern>> PATTERNS = KEYWORDS.entrySet().stream()
			.collect(java.util.stream.Collectors.toUnmodifiableMap(
					Map.Entry::getKey,
					entry -> entry.getValue().stream()
							.map(InteractionTagMatcher::wordBoundaryPattern)
							.toList()
			));

	private static Pattern wordBoundaryPattern(String keyword) {
		return Pattern.compile(
				"\\b" + Pattern.quote(keyword) + "\\b",
				Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
		);
	}

	private InteractionTagMatcher() {
	}

	static Set<InteractionTag> match(String keyIngredients) {
		if (!StringUtils.hasText(keyIngredients)) {
			return Set.of();
		}

		Set<InteractionTag> matched = new LinkedHashSet<>();
		PATTERNS.forEach((tag, patterns) -> {
			boolean found = patterns.stream().anyMatch(pattern -> pattern.matcher(keyIngredients).find());
			if (found) {
				matched.add(tag);
			}
		});

		return Set.copyOf(matched);
	}
}
