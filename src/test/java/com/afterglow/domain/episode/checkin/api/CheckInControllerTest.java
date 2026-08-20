package com.afterglow.domain.episode.checkin.api;

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

import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.intake.api.SymptomRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckInControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void IMPROVED_상태를_저장하면_200을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);

		mockMvc.perform(put("/api/episodes/" + episodeId + "/check-ins/2026-08-17")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CheckInRequest(CheckInStatus.IMPROVED))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.checkInDate").value("2026-08-17"))
				.andExpect(jsonPath("$.status").value("IMPROVED"));
	}

	@Test
	void SAME_상태를_저장하면_200을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);

		mockMvc.perform(put("/api/episodes/" + episodeId + "/check-ins/2026-08-17")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CheckInRequest(CheckInStatus.SAME))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SAME"));
	}

	@Test
	void WORSE_상태를_저장하면_200을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);

		mockMvc.perform(put("/api/episodes/" + episodeId + "/check-ins/2026-08-17")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CheckInRequest(CheckInStatus.WORSE))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("WORSE"));
	}

	@Test
	void 저장한_기록을_조회할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);
		record(token, episodeId, "2026-08-17", CheckInStatus.SAME);

		mockMvc.perform(get("/api/episodes/" + episodeId + "/check-ins")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].checkInDate").value("2026-08-17"))
				.andExpect(jsonPath("$[0].status").value("SAME"));
	}

	@Test
	void 다른_계정의_에피소드에_기록하면_404를_받는다() throws Exception {
		String ownerToken = createAnonymousAccountToken();
		Long episodeId = createEpisode(ownerToken);
		String otherToken = createAnonymousAccountToken();

		mockMvc.perform(put("/api/episodes/" + episodeId + "/check-ins/2026-08-17")
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CheckInRequest(CheckInStatus.IMPROVED))))
				.andExpect(status().isNotFound());
	}

	@Test
	void 다른_계정의_에피소드_기록을_조회하면_404를_받는다() throws Exception {
		String ownerToken = createAnonymousAccountToken();
		Long episodeId = createEpisode(ownerToken);
		String otherToken = createAnonymousAccountToken();

		mockMvc.perform(get("/api/episodes/" + episodeId + "/check-ins")
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void 존재하지_않는_에피소드에_기록하면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(put("/api/episodes/999999999/check-ins/2026-08-17")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CheckInRequest(CheckInStatus.IMPROVED))))
				.andExpect(status().isNotFound());
	}

	@Test
	void 같은_에피소드의_다른_날짜는_각각_저장된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);
		record(token, episodeId, "2026-08-17", CheckInStatus.SAME);
		record(token, episodeId, "2026-08-18", CheckInStatus.IMPROVED);

		mockMvc.perform(get("/api/episodes/" + episodeId + "/check-ins")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].checkInDate").value("2026-08-17"))
				.andExpect(jsonPath("$[1].checkInDate").value("2026-08-18"));
	}

	@Test
	void 같은_날짜에_다시_기록하면_최신_응답으로_덮어쓴다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);
		record(token, episodeId, "2026-08-17", CheckInStatus.SAME);
		record(token, episodeId, "2026-08-17", CheckInStatus.WORSE);

		mockMvc.perform(get("/api/episodes/" + episodeId + "/check-ins")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].status").value("WORSE"));
	}

	@Test
	void 인증_없이_기록하면_401을_받는다() throws Exception {
		mockMvc.perform(put("/api/episodes/1/check-ins/2026-08-17")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CheckInRequest(CheckInStatus.IMPROVED))))
				.andExpect(status().isUnauthorized());
	}

	private void record(String token, Long episodeId, String date, CheckInStatus status) throws Exception {
		mockMvc.perform(put("/api/episodes/" + episodeId + "/check-ins/" + date)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CheckInRequest(status))))
				.andExpect(status().isOk());
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
}
