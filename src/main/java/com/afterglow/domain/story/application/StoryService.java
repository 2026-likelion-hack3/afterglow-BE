package com.afterglow.domain.story.application;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.story.api.StoryLikeResponse;
import com.afterglow.domain.story.api.StoryResponse;
import com.afterglow.domain.story.domain.LifeStage;
import com.afterglow.domain.story.domain.SituationTag;
import com.afterglow.domain.story.domain.Story;
import com.afterglow.domain.story.domain.StoryLike;
import com.afterglow.domain.story.domain.StoryLikeRepository;
import com.afterglow.domain.story.domain.StoryRepository;
import com.afterglow.domain.story.domain.SymptomTag;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.afterglow.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

	private final StoryRepository storyRepository;
	private final StoryLikeRepository storyLikeRepository;

	/**
	 * 최신순 목록 조회(비로그인 허용). symptomTags/situationTags는 선택 — 각각 null/빈 집합이면 그
	 * 종류는 필터링하지 않는다. 필터가 있으면 "선택한 태그를 모두 만족하는 글만" 보여준다(Manyfast
	 * F-LBYUNJ 확정 규칙 원문: "여러 태그를 선택하면 모두 만족하는 글만 보여준다") — symptomTags와
	 * situationTags 사이도 같은 AND다. 데이터 규모가 작아(해커톤 데모 seed 수준) 태그 필터는 DB 쿼리로
	 * 새로 만들지 않고 이미 있는 {@code findAllByOrderByCreatedAtDescIdDesc()} 결과에 Java
	 * {@code Set.containsAll}로 적용한다 — 새 repository 메서드/쿼리가 필요 없다.
	 *
	 * <p>몇 개까지 동시에 겹쳐 선택할 수 있는지(예: 증상 1개+상황 1개 vs 최대 3개 vs 무제한)는 Manyfast에
	 * 아직 답변되지 않은 질문으로 남아있어(F-LBYUNJ questions), 이 메서드는 개수 제한을 임의로 걸지
	 * 않는다 — 확정된 "모두 만족" AND 규칙만 그대로 적용한다.
	 */
	public List<StoryResponse> getStories(Long accountId, Set<SymptomTag> symptomTags, Set<SituationTag> situationTags) {
		Set<SymptomTag> requiredSymptomTags = symptomTags == null ? Set.of() : symptomTags;
		Set<SituationTag> requiredSituationTags = situationTags == null ? Set.of() : situationTags;

		List<Story> stories = storyRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
			.filter(story -> story.getSymptomTags().containsAll(requiredSymptomTags))
			.filter(story -> story.getSituationTags().containsAll(requiredSituationTags))
			.toList();

		Set<Long> likedStoryIds = likedStoryIds(accountId, stories.stream().map(Story::getId).toList());

		return stories.stream()
			.map(story -> StoryResponse.from(story, likedStoryIds.contains(story.getId())))
			.toList();
	}

	public StoryResponse getStory(Long accountId, Long storyId) {
		Story story = storyRepository.findById(storyId)
			.orElseThrow(() -> new NotFoundException("이야기를 찾을 수 없습니다."));

		boolean likedByMe = accountId != null
			&& storyLikeRepository.findByAccountIdAndStoryId(accountId, storyId).isPresent();

		return StoryResponse.from(story, likedByMe);
	}

	/**
	 * 공감 토글(2026-08-20, 확정 규칙) — account당 story당 공감은 최대 1개다({@code story_like} unique
	 * 제약이 최종 방어선). 이미 공감했으면 취소(row 삭제 + likeCount 감소), 아니면 추가(row 생성 +
	 * likeCount 증가) — {@link StoryLikeRepository}의 존재 여부 조회 결과 하나로 두 분기를 가른다.
	 * {@code StoryLike} row 생성/삭제와 {@code Story.increase/decreaseLikeCount()}가 항상 같은
	 * 트랜잭션 안에서 한 쌍으로만 일어나므로(이 메서드가 유일한 진입점) likeCount와 실제 공감 이력이
	 * 어긋나지 않는다.
	 */
	@Transactional
	public StoryLikeResponse toggleLike(Long accountId, Long storyId) {
		Story story = storyRepository.findById(storyId)
			.orElseThrow(() -> new NotFoundException("이야기를 찾을 수 없습니다."));

		Optional<StoryLike> existing = storyLikeRepository.findByAccountIdAndStoryId(accountId, storyId);
		boolean liked;
		if (existing.isPresent()) {
			storyLikeRepository.delete(existing.get());
			story.decreaseLikeCount();
			liked = false;
		} else {
			storyLikeRepository.save(StoryLike.create(accountId, storyId));
			story.increaseLikeCount();
			liked = true;
		}

		return new StoryLikeResponse(liked, story.getLikeCount());
	}

	private Set<Long> likedStoryIds(Long accountId, List<Long> storyIds) {
		if (accountId == null || storyIds.isEmpty()) {
			return Set.of();
		}
		return storyLikeRepository.findByAccountIdAndStoryIdIn(accountId, storyIds).stream()
			.map(StoryLike::getStoryId)
			.collect(Collectors.toSet());
	}

	@Transactional
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
