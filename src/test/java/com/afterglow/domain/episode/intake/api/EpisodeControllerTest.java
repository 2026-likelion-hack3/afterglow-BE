package com.afterglow.domain.episode.intake.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.OnsetPeriod;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EpisodeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 증상을_입력하면_에피소드가_생성된다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(post("/api/episodes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SymptomRequest(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.episodeId").isNotEmpty());
	}

	@Test
	void 인증_없이_에피소드를_생성하면_401을_받는다() throws Exception {
		mockMvc.perform(post("/api/episodes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SymptomRequest(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 삭제된_계정으로_에피소드를_생성하면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		mockMvc.perform(delete("/api/accounts/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/episodes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SymptomRequest(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE))))
				.andExpect(status().isNotFound());
	}

	@Test
	void 범위를_벗어난_angle이면_400을_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(post("/api/episodes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SymptomRequest(361.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 문진을_제출하면_204를_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/intake")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new IntakeRequest(OnsetPeriod.TODAY, java.util.Set.of(BodyPart.CHEEK), "새 앰플", null))))
				.andExpect(status().isNoContent());
	}

	@Test
	void 다른_계정의_에피소드에_문진을_제출하면_404를_받는다() throws Exception {
		String ownerToken = createAnonymousAccountToken();
		Long episodeId = createEpisode(ownerToken);
		String otherToken = createAnonymousAccountToken();

		mockMvc.perform(post("/api/episodes/" + episodeId + "/intake")
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new IntakeRequest(OnsetPeriod.TODAY, java.util.Set.of(BodyPart.CHEEK), null, null))))
				.andExpect(status().isNotFound());
	}

	@Test
	void 이미_완료된_문진을_다시_제출하면_409를_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);
		submitIntake(token, episodeId);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/intake")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new IntakeRequest(OnsetPeriod.TODAY, java.util.Set.of(BodyPart.CHEEK), null, null))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_EPISODE_STATE"));
	}

	@Test
	void 전체_부위와_다른_부위를_같이_보내면_400을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/intake")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new IntakeRequest(
								OnsetPeriod.TODAY, java.util.Set.of(BodyPart.WHOLE_FACE, BodyPart.CHEEK), null, null))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	private String createAnonymousAccountToken() throws Exception {
		String response = mockMvc.perform(post("/api/accounts/anonymous"))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("accessToken").asText();
	}

	private Long createEpisode(String token) throws Exception {
		String response = mockMvc.perform(post("/api/episodes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SymptomRequest(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE))))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("episodeId").asLong();
	}

	private void submitIntake(String token, Long episodeId) throws Exception {
		mockMvc.perform(post("/api/episodes/" + episodeId + "/intake")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(
						new IntakeRequest(OnsetPeriod.TODAY, java.util.Set.of(BodyPart.CHEEK), null, null))))
				.andExpect(status().isNoContent());
	}
}
