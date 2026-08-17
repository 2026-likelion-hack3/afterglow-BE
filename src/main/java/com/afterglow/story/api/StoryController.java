package com.afterglow.story.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.story.application.StoryService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StoryController {

	private final StoryService storyService;

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
}
