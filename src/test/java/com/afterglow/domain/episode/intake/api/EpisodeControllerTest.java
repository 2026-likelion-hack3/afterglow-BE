package com.afterglow.domain.episode.intake.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.checkin.domain.CheckIn;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.OnsetPeriod;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.routine.domain.Routine;
import com.afterglow.domain.episode.routine.domain.RoutineItem;
import com.afterglow.domain.episode.routine.domain.RoutineRepository;
import com.afterglow.domain.episode.routine.domain.RoutineTimeSlot;
import com.afterglow.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EpisodeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RoutineRepository routineRepository;

	@Autowired
	private CheckInRepository checkInRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

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

	// ------------------------------------------------------------------
	// E1(기록 목록, 2026-08-20) — 새 판정 없이 기존 Episode/Analysis/Routine/CheckIn을 조합만 한다.
	// ------------------------------------------------------------------

	@Test
	void 목록_조회는_최신순으로_반환된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long firstId = createEpisode(token);
		Long secondId = createEpisode(token);

		mockMvc.perform(get("/api/episodes").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].episodeId").value(secondId))
			.andExpect(jsonPath("$[1].episodeId").value(firstId));
	}

	@Test
	void 인증_없이_목록을_조회하면_401을_받는다() throws Exception {
		mockMvc.perform(get("/api/episodes"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 다른_계정의_에피소드는_목록에_보이지_않는다() throws Exception {
		String ownerToken = createAnonymousAccountToken();
		createEpisode(ownerToken);
		String otherToken = createAnonymousAccountToken();

		mockMvc.perform(get("/api/episodes").header("Authorization", "Bearer " + otherToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void 문진만_끝난_에피소드는_analysis_routine_관련_필드가_모두_null이고_productIds는_빈_배열이다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);
		submitIntake(token, episodeId);

		mockMvc.perform(get("/api/episodes").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].episodeId").value(episodeId))
			.andExpect(jsonPath("$[0].status").value("INTAKE_COMPLETED"))
			.andExpect(jsonPath("$[0].analysisHold").value(nullValue()))
			.andExpect(jsonPath("$[0].analysisCauseType").value(nullValue()))
			.andExpect(jsonPath("$[0].routineStatus").value(nullValue()))
			.andExpect(jsonPath("$[0].day1Status").value(nullValue()))
			.andExpect(jsonPath("$[0].day3Verdict").value(nullValue()))
			.andExpect(jsonPath("$[0].productIds").isEmpty());
	}

	/** raw data(Vanity/Tracking)가 전혀 없는 익명 계정이라 분석은 항상 HOLD/NO_TARGET이다(기존 EpisodeAnalysisServiceTest와 동일 전제). */
	@Test
	void 분석까지_완료된_에피소드는_hold_상태가_목록에_반영된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long episodeId = createEpisode(token);
		submitIntake(token, episodeId);

		mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/episodes").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].status").value("ANALYZED"))
			.andExpect(jsonPath("$[0].analysisHold").value(true))
			.andExpect(jsonPath("$[0].analysisCauseType").value(nullValue()))
			.andExpect(jsonPath("$[0].routineStatus").value(nullValue()));
	}

	@Test
	void routine과_checkin이_있으면_day_상태와_productIds가_목록에_반영된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);
		Long episodeId = createEpisode(token);
		submitIntake(token, episodeId);
		mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis").header("Authorization", "Bearer " + token));

		LocalDate startDate = LocalDate.now();
		routineRepository.save(Routine.create(episodeId, accountId, startDate,
			List.of(RoutineItem.continueItem(1, RoutineTimeSlot.MORNING, 999L))));
		checkInRepository.save(CheckIn.create(episodeId, startDate, CheckInStatus.SAME));

		mockMvc.perform(get("/api/episodes").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].routineStatus").value("ACTIVE"))
			.andExpect(jsonPath("$[0].day1Status").value("SAME"))
			.andExpect(jsonPath("$[0].day2Status").value(nullValue()))
			.andExpect(jsonPath("$[0].productIds[0]").value(999))
			// Day1만 응답(2일 이하)이고 나빠짐이 없으므로 Day3JudgmentEngine 확정 규칙상 WITHHELD다.
			.andExpect(jsonPath("$[0].day3Verdict").value("WITHHELD"));
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
