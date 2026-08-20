package com.afterglow.domain.story.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.story.domain.LifeStage;
import com.afterglow.domain.story.domain.SituationTag;
import com.afterglow.domain.story.domain.StoryLike;
import com.afterglow.domain.story.domain.StoryLikeRepository;
import com.afterglow.domain.story.domain.SymptomTag;
import com.afterglow.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private StoryLikeRepository storyLikeRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void 로그인한_사용자는_증상_태그가_있는_글을_작성할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();

		StoryCreateRequest request = new StoryCreateRequest(
			"요즘 피부가 너무 건조해요",
			"잠을 못 잔 날에 특히 건조한 느낌이 있었어요.",
			Set.of(SymptomTag.DRYNESS_TIGHTNESS),
			Set.of(SituationTag.SLEEP_DEPRIVATION),
			LifeStage.MENOPAUSE,
			true
		);

		mockMvc.perform(post("/api/stories")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isNumber());
	}

	@Test
	void 상황_태그가_없어도_글을_작성할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();

		StoryCreateRequest request = new StoryCreateRequest(
			"건조함이 고민이에요",
			"요즘 피부가 많이 건조합니다.",
			Set.of(SymptomTag.DRYNESS_TIGHTNESS),
			null,
			null,
			false
		);

		mockMvc.perform(post("/api/stories")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isNumber());
	}

	@Test
	void 증상_태그가_없으면_글을_작성할_수_없다() throws Exception {
		String token = createAnonymousAccountToken();

		StoryCreateRequest request = new StoryCreateRequest(
			"태그 없는 글",
			"증상 태그가 없습니다.",
			Set.of(),
			Set.of(SituationTag.STRESS),
			null,
			false
		);

		mockMvc.perform(post("/api/stories")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 인증_없이_글을_작성하면_401을_받는다() throws Exception {
		StoryCreateRequest request = new StoryCreateRequest(
			"테스트 글",
			"테스트 내용입니다.",
			Set.of(SymptomTag.DRYNESS_TIGHTNESS),
			null,
			null,
			false
		);

		mockMvc.perform(post("/api/stories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
	}

	// ------------------------------------------------------------------
	// RC1: 목록/상세 조회(비로그인 허용, 태그 필터 없이 전체 최신순)
	// ------------------------------------------------------------------

	@Test
	void 목록_조회는_최신순으로_반환된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long firstId = createStory(token, "첫 번째 글", Set.of(SymptomTag.ITCHING));
		Long secondId = createStory(token, "두 번째 글", Set.of(SymptomTag.STINGING));

		mockMvc.perform(get("/api/stories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(secondId))
			.andExpect(jsonPath("$[1].id").value(firstId));
	}

	@Test
	void 목록_조회는_비로그인으로도_가능하다() throws Exception {
		mockMvc.perform(get("/api/stories"))
			.andExpect(status().isOk());
	}

	@Test
	void 상세_조회는_ID로_단건을_반환한다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "상세 조회용 글", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(get("/api/stories/" + storyId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(storyId))
			.andExpect(jsonPath("$.title").value("상세 조회용 글"));
	}

	@Test
	void 상세_조회는_비로그인으로도_가능하다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "비로그인 조회용 글", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(get("/api/stories/" + storyId))
			.andExpect(status().isOk());
	}

	@Test
	void 존재하지_않는_이야기를_상세_조회하면_404를_받는다() throws Exception {
		mockMvc.perform(get("/api/stories/999999"))
			.andExpect(status().isNotFound());
	}

	@Test
	void lifeStagePublic이_true면_상세_응답에_lifeStage가_노출된다() throws Exception {
		String token = createAnonymousAccountToken();
		StoryCreateRequest request = new StoryCreateRequest(
			"생애 단계 공개",
			"본문입니다.",
			Set.of(SymptomTag.REDNESS),
			null,
			LifeStage.MENOPAUSE,
			true
		);
		Long storyId = createStory(token, request);

		mockMvc.perform(get("/api/stories/" + storyId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lifeStage").value("MENOPAUSE"));
	}

	@Test
	void lifeStagePublic이_false면_상세_응답에_lifeStage가_노출되지_않는다() throws Exception {
		String token = createAnonymousAccountToken();
		StoryCreateRequest request = new StoryCreateRequest(
			"생애 단계 비공개",
			"본문입니다.",
			Set.of(SymptomTag.REDNESS),
			null,
			LifeStage.MENOPAUSE,
			false
		);
		Long storyId = createStory(token, request);

		mockMvc.perform(get("/api/stories/" + storyId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lifeStage").value(nullValue()));
	}

	// ------------------------------------------------------------------
	// 태그 필터(2026-08-20) — symptomTags/situationTags를 함께 걸면 선택한 태그를 모두 만족하는 글만
	// 반환한다(AND, Manyfast F-LBYUNJ 확정 규칙).
	// ------------------------------------------------------------------

	@Test
	void symptomTags_필터는_선택한_태그를_모두_가진_글만_반환한다() throws Exception {
		String token = createAnonymousAccountToken();
		Long matchId = createStory(token, "가려움+따가움", Set.of(SymptomTag.ITCHING, SymptomTag.STINGING));
		createStory(token, "가려움만", Set.of(SymptomTag.ITCHING));

		mockMvc.perform(get("/api/stories").param("symptomTags", "ITCHING", "STINGING"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(matchId));
	}

	@Test
	void situationTags_필터는_선택한_태그를_모두_가진_글만_반환한다() throws Exception {
		String token = createAnonymousAccountToken();
		StoryCreateRequest matched = new StoryCreateRequest(
			"수면부족+스트레스", "본문입니다.", Set.of(SymptomTag.REDNESS),
			Set.of(SituationTag.SLEEP_DEPRIVATION, SituationTag.STRESS), null, false);
		Long matchId = createStory(token, matched);
		createStory(token, "상황 태그 없음", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(get("/api/stories").param("situationTags", "SLEEP_DEPRIVATION", "STRESS"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(matchId));
	}

	@Test
	void symptomTags와_situationTags를_함께_주면_둘_다_만족하는_글만_반환된다() throws Exception {
		String token = createAnonymousAccountToken();
		StoryCreateRequest matched = new StoryCreateRequest(
			"조건 모두 만족", "본문입니다.", Set.of(SymptomTag.ITCHING), Set.of(SituationTag.STRESS), null, false);
		Long matchId = createStory(token, matched);

		StoryCreateRequest symptomOnly = new StoryCreateRequest(
			"증상만 일치", "본문입니다.", Set.of(SymptomTag.ITCHING), Set.of(SituationTag.ALCOHOL), null, false);
		createStory(token, symptomOnly);

		mockMvc.perform(get("/api/stories")
				.param("symptomTags", "ITCHING")
				.param("situationTags", "STRESS"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(matchId));
	}

	@Test
	void 필터에_해당하는_글이_없으면_빈_배열을_반환한다() throws Exception {
		String token = createAnonymousAccountToken();
		createStory(token, "가려움만", Set.of(SymptomTag.ITCHING));

		mockMvc.perform(get("/api/stories").param("symptomTags", "STINGING"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	// ------------------------------------------------------------------
	// SymptomTag DB contract 버그 수정 검증(2026-08-20) — @Transactional 테스트 롤백이 flush 전에
	// 일어나 기존 4개 테스트로는 CHECK 제약 위반을 못 잡았던 문제를, 이 테스트에서는 명시적으로
	// entityManager.flush()를 호출해 실제 DB round-trip까지 검증한다.
	// ------------------------------------------------------------------

	@Test
	void DRYNESS_TIGHTNESS_태그는_실제_DB_flush까지_성공한다() throws Exception {
		String token = createAnonymousAccountToken();
		createStory(token, "건조·당김 태그 flush 검증", Set.of(SymptomTag.DRYNESS_TIGHTNESS));

		assertThatCode(() -> entityManager.flush()).doesNotThrowAnyException();
	}

	@Test
	void TROUBLE_태그는_실제_DB_flush까지_성공한다() throws Exception {
		String token = createAnonymousAccountToken();
		createStory(token, "트러블 태그 flush 검증", Set.of(SymptomTag.TROUBLE));

		assertThatCode(() -> entityManager.flush()).doesNotThrowAnyException();
	}

	// ------------------------------------------------------------------
	// 공감 토글(2026-08-20 확정 규칙) — account당 story당 최대 1개, 처음 누르면 추가, 다시 누르면 취소,
	// 사용자마다 독립적.
	// ------------------------------------------------------------------

	@Test
	void 처음_공감을_누르면_추가되고_likeCount가_1이_된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "공감 테스트 글", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(post("/api/stories/" + storyId + "/likes").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.liked").value(true))
			.andExpect(jsonPath("$.likeCount").value(1));
	}

	@Test
	void 같은_사용자가_다시_누르면_공감이_취소되고_likeCount가_0이_된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "공감 취소 테스트", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(post("/api/stories/" + storyId + "/likes").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/stories/" + storyId + "/likes").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.liked").value(false))
			.andExpect(jsonPath("$.likeCount").value(0));
	}

	@Test
	void 같은_계정_같은_글에_대한_중복_StoryLike_row는_DB_unique_제약으로_거부된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "중복 방지 테스트", Set.of(SymptomTag.REDNESS));
		Long accountId = jwtTokenProvider.parseAccountId(token);

		storyLikeRepository.save(StoryLike.create(accountId, storyId));
		entityManager.flush();

		// IDENTITY 채번 전략은 save() 시점에 즉시 INSERT하므로(flush를 기다리지 않는다), 위반은 save() 호출
		// 자체에서 발생한다.
		assertThatThrownBy(() -> storyLikeRepository.save(StoryLike.create(accountId, storyId)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 서로_다른_두_사용자가_각각_공감하면_likeCount는_2다() throws Exception {
		String tokenA = createAnonymousAccountToken();
		String tokenB = createAnonymousAccountToken();
		Long storyId = createStory(tokenA, "여러 사용자 공감", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(post("/api/stories/" + storyId + "/likes").header("Authorization", "Bearer " + tokenA))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.likeCount").value(1));

		mockMvc.perform(post("/api/stories/" + storyId + "/likes").header("Authorization", "Bearer " + tokenB))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.likeCount").value(2));
	}

	@Test
	void 둘_중_한_명만_공감을_취소하면_likeCount는_1이다() throws Exception {
		String tokenA = createAnonymousAccountToken();
		String tokenB = createAnonymousAccountToken();
		Long storyId = createStory(tokenA, "부분 취소 테스트", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(post("/api/stories/" + storyId + "/likes").header("Authorization", "Bearer " + tokenA));
		mockMvc.perform(post("/api/stories/" + storyId + "/likes").header("Authorization", "Bearer " + tokenB));

		mockMvc.perform(post("/api/stories/" + storyId + "/likes").header("Authorization", "Bearer " + tokenA))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.liked").value(false))
			.andExpect(jsonPath("$.likeCount").value(1));
	}

	@Test
	void 인증_없이_공감을_시도하면_401을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "인증 필요 테스트", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(post("/api/stories/" + storyId + "/likes"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 공감하지_않은_글의_likedByMe는_false다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "공감 없는 글", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(get("/api/stories/" + storyId).header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.likedByMe").value(false));
	}

	@Test
	void 공감한_글을_상세_조회하면_likedByMe가_true다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "공감한 글", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(post("/api/stories/" + storyId + "/likes").header("Authorization", "Bearer " + token));

		mockMvc.perform(get("/api/stories/" + storyId).header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.likedByMe").value(true));
	}

	@Test
	void 비로그인으로_조회하면_likedByMe는_항상_false다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "비로그인 조회", Set.of(SymptomTag.REDNESS));
		mockMvc.perform(post("/api/stories/" + storyId + "/likes").header("Authorization", "Bearer " + token));

		mockMvc.perform(get("/api/stories/" + storyId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.likedByMe").value(false));
	}

	// ------------------------------------------------------------------
	// 댓글 기본 기능(2026-08-20, Figma G2:글상세 node 967:277 확인) — 작성/조회만. 수정/삭제/대댓글/
	// 좋아요/신고는 범위 밖. 정렬은 오래된 순(Figma 확인, Story 목록의 최신순과 반대). 작성자 표시 필드는
	// 없음(lifeStage 미해결 문제와 겹쳐 이번 범위에서 보류 — StoryComment 참고).
	// ------------------------------------------------------------------

	@Test
	void 댓글_작성에_성공하면_commentId를_반환한다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "댓글 테스트 글", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(post("/api/stories/" + storyId + "/comments")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new StoryCommentCreateRequest("저도 비슷한 경험이 있어요."))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isNumber());
	}

	@Test
	void 인증_없이_댓글을_작성하면_401을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "인증 없는 댓글 테스트", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(post("/api/stories/" + storyId + "/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new StoryCommentCreateRequest("댓글"))))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 존재하지_않는_이야기에_댓글을_작성하면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(post("/api/stories/999999/comments")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new StoryCommentCreateRequest("댓글"))))
			.andExpect(status().isNotFound());
	}

	@Test
	void 빈_content로_댓글을_작성하면_400을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "blank 검증용 글", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(post("/api/stories/" + storyId + "/comments")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new StoryCommentCreateRequest(""))))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 댓글_목록을_조회할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "댓글 목록 테스트", Set.of(SymptomTag.REDNESS));
		createComment(token, storyId, "첫 댓글");

		mockMvc.perform(get("/api/stories/" + storyId + "/comments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].content").value("첫 댓글"));
	}

	@Test
	void 댓글_목록_조회는_비로그인으로도_가능하다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "비로그인 댓글 조회 테스트", Set.of(SymptomTag.REDNESS));
		createComment(token, storyId, "댓글");

		mockMvc.perform(get("/api/stories/" + storyId + "/comments"))
			.andExpect(status().isOk());
	}

	@Test
	void 댓글이_없는_이야기는_빈_배열을_반환한다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "댓글 없는 글", Set.of(SymptomTag.REDNESS));

		mockMvc.perform(get("/api/stories/" + storyId + "/comments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void 존재하지_않는_이야기의_댓글_목록을_조회하면_404를_받는다() throws Exception {
		mockMvc.perform(get("/api/stories/999999/comments"))
			.andExpect(status().isNotFound());
	}

	/** Figma 순서(2일 전 → 1일 전 → 어제)와 같은 방향 — 먼저 쓴 댓글이 목록 맨 앞에 온다. */
	@Test
	void 여러_댓글은_오래된_순으로_반환된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long storyId = createStory(token, "댓글 정렬 테스트", Set.of(SymptomTag.REDNESS));
		Long firstCommentId = createComment(token, storyId, "먼저 쓴 댓글");
		Long secondCommentId = createComment(token, storyId, "나중에 쓴 댓글");

		mockMvc.perform(get("/api/stories/" + storyId + "/comments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].commentId").value(firstCommentId))
			.andExpect(jsonPath("$[1].commentId").value(secondCommentId));
	}

	private Long createComment(String token, Long storyId, String content) throws Exception {
		String response = mockMvc.perform(post("/api/stories/" + storyId + "/comments")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new StoryCommentCreateRequest(content))))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return Long.valueOf(response);
	}

	private Long createStory(String token, String title, Set<SymptomTag> symptomTags) throws Exception {
		return createStory(token, new StoryCreateRequest(title, "본문입니다.", symptomTags, null, null, false));
	}

	private Long createStory(String token, StoryCreateRequest request) throws Exception {
		String response = mockMvc.perform(post("/api/stories")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return Long.valueOf(response);
	}

	private String createAnonymousAccountToken() throws Exception {
		String response = mockMvc.perform(post("/api/accounts/anonymous"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		return json.get("accessToken").asText();
	}
}
