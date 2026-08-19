package com.afterglow.domain.episode.routine.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.checkin.api.CheckInRequest;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.OnsetPeriod;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.intake.api.IntakeRequest;
import com.afterglow.domain.episode.intake.api.SymptomRequest;
import com.afterglow.domain.episode.routine.domain.RoutineItemUsage;
import com.afterglow.domain.episode.routine.domain.RoutineTimeSlot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoutineControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 정상_요청이면_루틴이_생성되고_201을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.days.length()").value(3))
				.andExpect(jsonPath("$.days[0].dayNumber").value(1))
				.andExpect(jsonPath("$.days[0].items[0].timeSlot").value("MORNING"))
				.andExpect(jsonPath("$.days[0].items[0].productId").value(100))
				.andExpect(jsonPath("$.discontinuedProductIds[0]").value(999));
	}

	@Test
	void intake를_끝내지_않은_에피소드면_루틴_생성이_거부된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisodeOnly(token);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isConflict());
	}

	@Test
	void 같은_에피소드에_루틴을_두_번_만들면_거부된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isConflict());
	}

	@Test
	void 존재하지_않는_에피소드면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(post("/api/episodes/999999/routine")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isNotFound());
	}

	@Test
	void 다른_계정의_에피소드에는_루틴을_만들_수_없다() throws Exception {
		String ownerToken = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(ownerToken);

		String otherToken = createAnonymousAccountToken();

		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isNotFound());
	}

	@Test
	void 다른_계정은_루틴을_조회할_수_없다() throws Exception {
		String ownerToken = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(ownerToken);
		startRoutine(ownerToken, episodeId);

		String otherToken = createAnonymousAccountToken();

		mockMvc.perform(get("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void 인증_없이_루틴을_생성하면_401을_받는다() throws Exception {
		mockMvc.perform(post("/api/episodes/1/routine")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void CONTINUE_항목에_dayNumber가_없으면_400을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);

		RoutineCreateRequest invalid = new RoutineCreateRequest(List.of(
				new RoutineItemRequest(RoutineItemUsage.CONTINUE, null, RoutineTimeSlot.MORNING, 100L)));

		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalid)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void usage가_없으면_400을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);

		String body = """
				{"items": [{"dayNumber": 1, "timeSlot": "MORNING", "productId": 100}]}
				""";

		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 재조회해도_Day_구성과_날짜가_동일하게_유지된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);
		startRoutine(token, episodeId);

		String first = mockMvc.perform(get("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		String second = mockMvc.perform(get("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(first))
				.isEqualTo(objectMapper.readTree(second));
	}

	@Test
	void CheckIn_3일치가_모두_좋아졌다이면_Day3_결과가_MAINTAIN이다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);
		LocalDate startDate = startRoutine(token, episodeId);

		recordCheckIn(token, episodeId, startDate, CheckInStatus.IMPROVED);
		recordCheckIn(token, episodeId, startDate.plusDays(1), CheckInStatus.IMPROVED);
		recordCheckIn(token, episodeId, startDate.plusDays(2), CheckInStatus.IMPROVED);

		mockMvc.perform(get("/api/episodes/" + episodeId + "/routine/day3-result")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.verdict").value("MAINTAIN"));
	}

	@Test
	void CheckIn_중_하루라도_나빠졌다이면_Day3_결과가_STOP이다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);
		LocalDate startDate = startRoutine(token, episodeId);

		recordCheckIn(token, episodeId, startDate, CheckInStatus.IMPROVED);
		recordCheckIn(token, episodeId, startDate.plusDays(1), CheckInStatus.WORSE);

		mockMvc.perform(get("/api/episodes/" + episodeId + "/routine/day3-result")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.verdict").value("STOP"));
	}

	@Test
	void CheckIn이_2일_이하면_Day3_결과가_WITHHELD이다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);
		LocalDate startDate = startRoutine(token, episodeId);

		recordCheckIn(token, episodeId, startDate, CheckInStatus.IMPROVED);

		mockMvc.perform(get("/api/episodes/" + episodeId + "/routine/day3-result")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.verdict").value("WITHHELD"));
	}

	@Test
	void Day3_응답이_비슷하다이면_Day3_결과가_EXTEND이다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createIntakeCompletedEpisode(token);
		LocalDate startDate = startRoutine(token, episodeId);

		recordCheckIn(token, episodeId, startDate, CheckInStatus.IMPROVED);
		recordCheckIn(token, episodeId, startDate.plusDays(1), CheckInStatus.SAME);
		recordCheckIn(token, episodeId, startDate.plusDays(2), CheckInStatus.SAME);

		mockMvc.perform(get("/api/episodes/" + episodeId + "/routine/day3-result")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.verdict").value("EXTEND"));
	}

	private RoutineCreateRequest defaultRoutineRequest() {
		return new RoutineCreateRequest(List.of(
				new RoutineItemRequest(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, 100L),
				new RoutineItemRequest(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.EVENING, 200L),
				new RoutineItemRequest(RoutineItemUsage.CONTINUE, 2, RoutineTimeSlot.MORNING, 100L),
				new RoutineItemRequest(RoutineItemUsage.CONTINUE, 3, RoutineTimeSlot.MORNING, 100L),
				new RoutineItemRequest(RoutineItemUsage.DISCONTINUE, null, null, 999L)
		));
	}

	private LocalDate startRoutine(String token, Long episodeId) throws Exception {
		String response = mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return LocalDate.parse(objectMapper.readTree(response).get("startDate").asText());
	}

	private void recordCheckIn(String token, Long episodeId, LocalDate date, CheckInStatus status) throws Exception {
		mockMvc.perform(put("/api/episodes/" + episodeId + "/check-ins/" + date)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CheckInRequest(status))))
				.andExpect(status().isOk());
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
