package com.afterglow.domain.episode.analysis.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.OnsetPeriod;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.intake.api.IntakeRequest;
import com.afterglow.domain.episode.intake.api.SymptomRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EpisodeAnalysisControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 정상_요청이면_분석이_실행되고_200과_카드_3장을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hold").value(true))
				.andExpect(jsonPath("$.holdReason").value("NO_TARGET"))
				.andExpect(jsonPath("$.cards.length()").value(3))
				.andExpect(jsonPath("$.cards[0].type").value("WITHHELD"));
	}

	@Test
	void intake를_끝내지_않았으면_분석_실행이_409를_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisodeOnly(token);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());
	}

	@Test
	void 존재하지_않는_episode면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(post("/api/episodes/999999/analysis")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void 다른_계정의_episode는_분석할_수_없다() throws Exception {
		String ownerToken = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(ownerToken);

		String otherToken = createAnonymousAccountToken();

		mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis")
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void 인증_없이_분석을_실행하면_401을_받는다() throws Exception {
		mockMvc.perform(post("/api/episodes/1/analysis"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 분석_전에_결과를_조회하면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);

		mockMvc.perform(get("/api/episodes/" + episodeId + "/analysis")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void 분석_후_결과_조회는_실행_결과와_동일하다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/episodes/" + episodeId + "/analysis")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hold").value(true))
				.andExpect(jsonPath("$.holdReason").value("NO_TARGET"));
	}

	@Test
	void 같은_episode를_두_번_분석해도_같은_결과를_반환한다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);

		String first = mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		String second = mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(first))
				.isEqualTo(objectMapper.readTree(second));
	}

	@Test
	void 분석_후_explanation_조회는_기본_설정에서_FALLBACK_source로_200을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/episodes/" + episodeId + "/analysis/explanation")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.source").value("FALLBACK"))
				.andExpect(jsonPath("$.headline").isNotEmpty())
				.andExpect(jsonPath("$.summary").isNotEmpty())
				.andExpect(jsonPath("$.nextAction").isNotEmpty());
	}

	@Test
	void 분석_결과가_없으면_explanation_조회가_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);

		mockMvc.perform(get("/api/episodes/" + episodeId + "/analysis/explanation")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void 존재하지_않는_episode의_explanation은_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(get("/api/episodes/999999/analysis/explanation")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void 다른_계정의_episode는_explanation을_조회할_수_없다() throws Exception {
		String ownerToken = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(ownerToken);
		mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis")
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk());

		String otherToken = createAnonymousAccountToken();

		mockMvc.perform(get("/api/episodes/" + episodeId + "/analysis/explanation")
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void 인증_없이_explanation을_조회하면_401을_받는다() throws Exception {
		mockMvc.perform(get("/api/episodes/1/analysis/explanation"))
				.andExpect(status().isUnauthorized());
	}

	private Long createEpisodeOnly(String token) throws Exception {
		String response = mockMvc.perform(post("/api/episodes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new SymptomRequest(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("episodeId").asLong();
	}

	private Long createIntakeCompletedEpisode(String token) throws Exception {
		Long episodeId = createEpisodeOnly(token);
		mockMvc.perform(post("/api/episodes/" + episodeId + "/intake")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new IntakeRequest(OnsetPeriod.TODAY, Set.of(BodyPart.CHEEK), "새 앰플", null))))
				.andExpect(status().isNoContent());
		return episodeId;
	}

	private String createAnonymousAccountToken() throws Exception {
		String response = mockMvc.perform(post("/api/accounts/anonymous"))
				.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(response);
		return json.get("accessToken").asText();
	}
}
