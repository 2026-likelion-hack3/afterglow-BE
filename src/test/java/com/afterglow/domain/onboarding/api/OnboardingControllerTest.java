package com.afterglow.domain.onboarding.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.onboarding.domain.AgeRange;
import com.afterglow.domain.onboarding.domain.MenstrualStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OnboardingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 온보딩_기록이_없으면_모든_값이_null인_200을_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(get("/api/onboarding").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ageRange").isEmpty())
				.andExpect(jsonPath("$.menstrualStatus").isEmpty())
				.andExpect(jsonPath("$.onboardingCompletedAt").isEmpty());
	}

	@Test
	void 인증_없이_조회하면_401을_받는다() throws Exception {
		mockMvc.perform(get("/api/onboarding"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 연령대와_월경상태를_모두_제출하면_완료_시각과_함께_200을_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(put("/api/onboarding")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new OnboardingRequest(AgeRange.FIFTY_TO_FIFTY_FOUR, MenstrualStatus.IRREGULAR))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ageRange").value("FIFTY_TO_FIFTY_FOUR"))
				.andExpect(jsonPath("$.menstrualStatus").value("IRREGULAR"))
				.andExpect(jsonPath("$.onboardingCompletedAt").isNotEmpty());
	}

	@Test
	void 전체_스킵으로_제출해도_완료_시각과_함께_200을_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(put("/api/onboarding")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new OnboardingRequest(null, null))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ageRange").isEmpty())
				.andExpect(jsonPath("$.menstrualStatus").isEmpty())
				.andExpect(jsonPath("$.onboardingCompletedAt").isNotEmpty());
	}

	@Test
	void 제출한_값을_다시_조회할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();
		mockMvc.perform(put("/api/onboarding")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new OnboardingRequest(AgeRange.SIXTY_OR_OLDER, MenstrualStatus.PREFER_NOT_TO_ANSWER))))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/onboarding").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ageRange").value("SIXTY_OR_OLDER"))
				.andExpect(jsonPath("$.menstrualStatus").value("PREFER_NOT_TO_ANSWER"));
	}

	@Test
	void 인증_없이_제출하면_401을_받는다() throws Exception {
		mockMvc.perform(put("/api/onboarding")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new OnboardingRequest(AgeRange.SIXTY_OR_OLDER, MenstrualStatus.REGULAR))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 잘못된_enum_값으로_제출하면_400을_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(put("/api/onboarding")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"ageRange\":\"NOT_A_REAL_VALUE\",\"menstrualStatus\":null}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 삭제된_계정으로_조회하면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		mockMvc.perform(delete("/api/accounts/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/onboarding").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void 삭제된_계정으로_제출하면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		mockMvc.perform(delete("/api/accounts/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(put("/api/onboarding")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new OnboardingRequest(AgeRange.SIXTY_OR_OLDER, MenstrualStatus.REGULAR))))
				.andExpect(status().isNotFound());
	}

	private String createAnonymousAccountToken() throws Exception {
		String response = mockMvc.perform(post("/api/accounts/anonymous"))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("accessToken").asText();
	}
}
