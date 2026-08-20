package com.afterglow.domain.story.domain;

import com.afterglow.global.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * story에 달린 댓글 하나 — 기본 기능만(작성/조회). 수정/삭제/대댓글/좋아요/신고는 이번 범위 밖이라 그에
 * 필요한 상태(수정 여부, 부모 댓글, 좋아요 수, 숨김 등)를 두지 않는다.
 *
 * <p><b>작성자 표시(2026-08-20, Figma G2:글상세 node 967:277 확인)</b>: Figma 와이어프레임은 댓글마다
 * "폐경 3년차"/"이행기" 같은 lifeStage 스타일 라벨을 보여준다 — 그런데 이건 {@code Story} 자체의
 * lifeStage와 완전히 같은 미해결 문제에 걸린다(MenstrualStatus → LifeStage 매핑, "N년차" 계산에 필요한
 * 날짜 데이터 자체가 온보딩 스키마에 없음, lifeStage contract는 기획 답변 대기 중이라 이번 범위에서
 * 손대지 않기로 확정함). 그래서 이번 구현은 지시받은 스키마(story_id/account_id/content만) 그대로
 * {@code accountId}만 내부적으로 갖고 API 응답에는 작성자 식별/lifeStage 필드를 포함하지 않는다 —
 * 새 정책을 만든 게 아니라, 확정되지 않은 부분을 구현하지 않고 보고로 남겨둔 것이다.
 */
@Getter
@Entity
@Table(name = "story_comment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryComment extends BaseEntity {

	@Column(nullable = false)
	private Long storyId;

	@Column(nullable = false)
	private Long accountId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	private StoryComment(Long storyId, Long accountId, String content) {
		this.storyId = storyId;
		this.accountId = accountId;
		this.content = content;
	}

	public static StoryComment create(Long storyId, Long accountId, String content) {
		return new StoryComment(storyId, accountId, content);
	}
}
