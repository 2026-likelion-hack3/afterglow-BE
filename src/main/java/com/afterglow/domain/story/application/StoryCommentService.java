package com.afterglow.domain.story.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.story.api.StoryCommentResponse;
import com.afterglow.domain.story.domain.StoryComment;
import com.afterglow.domain.story.domain.StoryCommentRepository;
import com.afterglow.domain.story.domain.StoryRepository;
import com.afterglow.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryCommentService {

	private final StoryRepository storyRepository;
	private final StoryCommentRepository storyCommentRepository;

	/** 댓글 조회는 비로그인 허용. 없는 story는 404, 댓글이 없으면 빈 배열. */
	public List<StoryCommentResponse> getComments(Long storyId) {
		requireStoryExists(storyId);
		return storyCommentRepository.findByStoryIdOrderByCreatedAtAscIdAsc(storyId).stream()
			.map(StoryCommentResponse::from)
			.toList();
	}

	@Transactional
	public Long createComment(Long accountId, Long storyId, String content) {
		requireStoryExists(storyId);
		StoryComment comment = StoryComment.create(storyId, accountId, content);
		storyCommentRepository.save(comment);
		return comment.getId();
	}

	private void requireStoryExists(Long storyId) {
		storyRepository.findById(storyId)
			.orElseThrow(() -> new NotFoundException("이야기를 찾을 수 없습니다."));
	}
}
