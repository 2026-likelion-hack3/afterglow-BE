package com.afterglow.domain.story.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.story.domain.LifeStage;
import com.afterglow.domain.story.domain.SituationTag;
import com.afterglow.domain.story.domain.SymptomTag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 로그인한_사용자는_증상_태그가_있는_글을_작성할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();

		StoryCreateRequest request = new StoryCreateRequest(
			"요즘 피부가 너무 건조해요",
			"잠을 못 잔 날에 특히 건조한 느낌이 있었어요.",
			Set.of(SymptomTag.DRYNESS),
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
			Set.of(SymptomTag.DRYNESS),
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
			Set.of(SymptomTag.DRYNESS),
			null,
			null,
			false
		);

		mockMvc.perform(post("/api/stories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
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
