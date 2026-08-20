package com.afterglow.domain.story.api;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.domain.story.application.StoryCommentService;
import com.afterglow.domain.story.application.StoryService;
import com.afterglow.domain.story.domain.SituationTag;
import com.afterglow.domain.story.domain.SymptomTag;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StoryController {

	private final StoryService storyService;
	private final StoryCommentService storyCommentService;

	/**
	 * 최신순 목록. symptomTags/situationTags 쿼리 파라미터는 선택이며, 있으면 선택한 태그를 모두 만족하는
	 * 글만 반환한다(AND, Manyfast F-LBYUNJ 확정). 비로그인 조회 허용(SecurityConfig에서 GET만 permitAll)
	 * — accountId는 로그인 상태에서만 채워지고(비로그인이면 null), 각 항목의 likedByMe 계산에만 쓰인다.
	 */
	@GetMapping
	public ResponseEntity<List<StoryResponse>> getStories(
		@AuthenticationPrincipal Long accountId,
		@RequestParam(required = false) Set<SymptomTag> symptomTags,
		@RequestParam(required = false) Set<SituationTag> situationTags) {

		return ResponseEntity.ok(storyService.getStories(accountId, symptomTags, situationTags));
	}

	/** 비로그인 조회 허용. 존재하지 않는 id는 {@code NotFoundException}으로 404. */
	@GetMapping("/{storyId}")
	public ResponseEntity<StoryResponse> getStory(
		@AuthenticationPrincipal Long accountId,
		@PathVariable Long storyId) {

		return ResponseEntity.ok(storyService.getStory(accountId, storyId));
	}

	@PostMapping
	public ResponseEntity<Long> createStory(
		@AuthenticationPrincipal Long accountId,
		@Valid @RequestBody StoryCreateRequest request) {

		Long storyId = storyService.createStory(
			accountId,
			request.title(),
			request.content(),
			request.symptomTags(),
			request.situationTags(),
			request.lifeStage(),
			request.lifeStagePublic()
		);

		return ResponseEntity.ok(storyId);
	}

	/** 공감 토글 — 인증 필요(이 경로는 GET이 아니라 SecurityConfig의 permitAll 대상이 아니다). 처음 누르면 추가, 다시 누르면 취소. */
	@PostMapping("/{storyId}/likes")
	public ResponseEntity<StoryLikeResponse> toggleLike(
		@AuthenticationPrincipal Long accountId,
		@PathVariable Long storyId) {

		return ResponseEntity.ok(storyService.toggleLike(accountId, storyId));
	}

	/**
	 * 오래된 순(Figma G2:글상세 확인, 2026-08-20). 비로그인 조회 허용 — GET이라 SecurityConfig의
	 * "/api/stories/**" permitAll에 이미 포함된다(추가 설정 불필요). 없는 storyId는 404.
	 */
	@GetMapping("/{storyId}/comments")
	public ResponseEntity<List<StoryCommentResponse>> getComments(@PathVariable Long storyId) {
		return ResponseEntity.ok(storyCommentService.getComments(storyId));
	}

	/** 인증 필요(POST라 기존 anyRequest().authenticated()에 그대로 걸린다). 없는 storyId는 404. */
	@PostMapping("/{storyId}/comments")
	public ResponseEntity<Long> createComment(
		@AuthenticationPrincipal Long accountId,
		@PathVariable Long storyId,
		@Valid @RequestBody StoryCommentCreateRequest request) {

		Long commentId = storyCommentService.createComment(accountId, storyId, request.content());
		return ResponseEntity.ok(commentId);
	}
}
