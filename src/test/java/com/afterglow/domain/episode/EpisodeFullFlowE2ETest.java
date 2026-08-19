package com.afterglow.domain.episode;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.afterglow.domain.episode.routine.api.RoutineCreateRequest;
import com.afterglow.domain.episode.routine.api.RoutineItemRequest;
import com.afterglow.domain.episode.routine.domain.RoutineItemUsage;
import com.afterglow.domain.episode.routine.domain.RoutineTimeSlot;
import com.afterglow.domain.onboarding.api.OnboardingRequest;
import com.afterglow.domain.onboarding.domain.AgeRange;
import com.afterglow.domain.onboarding.domain.MenstrualStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 마감 전 핵심 E2E 안정성 확인용 — 새 production 로직 없이, 이미 구현된 API/service만으로
 * account → onboarding → episode → intake → analysis → routine → checkin → Day3 판정까지
 * 실제로 이어지는지 검증한다. 외부 OpenAI/Vision/Weather 호출은 없다(Vanity/Tracking 미연동이라
 * Analysis는 항상 HOLD(NO_TARGET) — 이번 마감 시점 기준 정상 상태이며 실패로 보지 않는다).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EpisodeFullFlowE2ETest {

	private static final String AUTH = "Authorization";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void account부터_Day3_판정까지_핵심_플로우가_실제_API_경로로_끝까지_이어진다() throws Exception {
		String token = createAnonymousAccountToken();
		String otherToken = createAnonymousAccountToken();

		// 1~2. 익명 Account 생성 + Onboarding 완료
		mockMvc.perform(put("/api/onboarding")
						.header(AUTH, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new OnboardingRequest(AgeRange.FIFTY_TO_FIFTY_FOUR, MenstrualStatus.IRREGULAR))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onboardingCompletedAt").isNotEmpty());

		// 3. Episode 생성
		String episodeResponse = mockMvc.perform(post("/api/episodes")
						.header(AUTH, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new SymptomRequest(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Long episodeId = objectMapper.readTree(episodeResponse).get("episodeId").asLong();

		// Intake 전 Analysis 불가
		mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis").header(AUTH, bearer(token)))
				.andExpect(status().isConflict());

		// Analysis 전(SYMPTOM_SELECTED 시점) Routine 불가
		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header(AUTH, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isConflict());

		// 다른 account는 intake를 진행할 수 없다
		mockMvc.perform(post("/api/episodes/" + episodeId + "/intake")
						.header(AUTH, bearer(otherToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new IntakeRequest(OnsetPeriod.TODAY, Set.of(BodyPart.CHEEK), "새 앰플", null))))
				.andExpect(status().isNotFound());

		// 4. Intake 완료
		mockMvc.perform(post("/api/episodes/" + episodeId + "/intake")
						.header(AUTH, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new IntakeRequest(OnsetPeriod.TODAY, Set.of(BodyPart.CHEEK), "새 앰플", null))))
				.andExpect(status().isNoContent());

		// Analysis 전(INTAKE_COMPLETED 시점) Routine 여전히 불가
		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header(AUTH, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isConflict());

		// 다른 account는 analysis를 실행할 수 없다
		mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis").header(AUTH, bearer(otherToken)))
				.andExpect(status().isNotFound());

		// 5. Analysis 실행 — Vanity/Tracking 미연동이라 HOLD(NO_TARGET)가 정상
		String analysisResponse = mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis")
						.header(AUTH, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hold").value(true))
				.andExpect(jsonPath("$.holdReason").value("NO_TARGET"))
				.andExpect(jsonPath("$.cards.length()").value(3))
				.andReturn().getResponse().getContentAsString();

		// idempotent 재호출 — 재실행 없이 같은 결과
		String analysisResponseAgain = mockMvc.perform(post("/api/episodes/" + episodeId + "/analysis")
						.header(AUTH, bearer(token)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(objectMapper.readTree(analysisResponseAgain)).isEqualTo(objectMapper.readTree(analysisResponse));

		// GET 조회 결과도 동일 — 6. Episode는 이 시점 ANALYZED (직접 조회 API는 없어 Routine 시작 가능 여부로 실증)
		mockMvc.perform(get("/api/episodes/" + episodeId + "/analysis").header(AUTH, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hold").value(true))
				.andExpect(jsonPath("$.holdReason").value("NO_TARGET"));

		// 다른 account는 Routine을 시작할 수 없다
		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header(AUTH, bearer(otherToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isNotFound());

		// 7. Analysis 완료(ANALYZED) 후 Routine 시작 — 성공
		String routineResponse = mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header(AUTH, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.days.length()").value(3))
				.andReturn().getResponse().getContentAsString();
		LocalDate startDate = LocalDate.parse(objectMapper.readTree(routineResponse).get("startDate").asText());

		// 같은 episode에 Routine 재생성 시도 — 기존 중복 방어 유지
		mockMvc.perform(post("/api/episodes/" + episodeId + "/routine")
						.header(AUTH, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(defaultRoutineRequest())))
				.andExpect(status().isConflict());

		// 8. Routine이 정확히 Day1~Day3(시작일 기준 연속 날짜)를 가짐
		mockMvc.perform(get("/api/episodes/" + episodeId + "/routine").header(AUTH, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.days.length()").value(3))
				.andExpect(jsonPath("$.days[0].dayNumber").value(1))
				.andExpect(jsonPath("$.days[0].date").value(startDate.toString()))
				.andExpect(jsonPath("$.days[1].dayNumber").value(2))
				.andExpect(jsonPath("$.days[1].date").value(startDate.plusDays(1).toString()))
				.andExpect(jsonPath("$.days[2].dayNumber").value(3))
				.andExpect(jsonPath("$.days[2].date").value(startDate.plusDays(2).toString()));

		// 다른 account는 Routine을 조회할 수 없다
		mockMvc.perform(get("/api/episodes/" + episodeId + "/routine").header(AUTH, bearer(otherToken)))
				.andExpect(status().isNotFound());

		// 9. 각 날짜(Day1/2/3)에 CheckIn 저장 — Day2에 WORSE를 넣어 "3개를 실제로 다 본다"는 걸 판정으로 증명한다
		//    (Day3만 봤다면 IMPROVED이므로 MAINTAIN이 나와야 하지만, WORSE 규칙이 실제로 적용되면 STOP이어야 한다)
		recordCheckIn(token, episodeId, startDate, CheckInStatus.IMPROVED);
		recordCheckIn(token, episodeId, startDate.plusDays(1), CheckInStatus.WORSE);
		recordCheckIn(token, episodeId, startDate.plusDays(2), CheckInStatus.IMPROVED);

		// 다른 account는 CheckIn을 기록/조회할 수 없다
		mockMvc.perform(put("/api/episodes/" + episodeId + "/check-ins/" + startDate)
						.header(AUTH, bearer(otherToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CheckInRequest(CheckInStatus.IMPROVED))))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/episodes/" + episodeId + "/check-ins").header(AUTH, bearer(otherToken)))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/episodes/" + episodeId + "/check-ins").header(AUTH, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));

		// 10. Day3 판정 — Day3JudgmentEngine이 저장된 3개 CheckIn을 실제로 소비했는지 검증(WORSE 규칙 반영 = STOP)
		mockMvc.perform(get("/api/episodes/" + episodeId + "/routine/day3-result").header(AUTH, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.verdict").value("STOP"));

		// 다른 account는 Day3 결과를 조회할 수 없다
		mockMvc.perform(get("/api/episodes/" + episodeId + "/routine/day3-result").header(AUTH, bearer(otherToken)))
				.andExpect(status().isNotFound());
	}

	private RoutineCreateRequest defaultRoutineRequest() {
		return new RoutineCreateRequest(List.of(
				new RoutineItemRequest(RoutineItemUsage.CONTINUE, 1, RoutineTimeSlot.MORNING, 100L),
				new RoutineItemRequest(RoutineItemUsage.CONTINUE, 2, RoutineTimeSlot.MORNING, 100L),
				new RoutineItemRequest(RoutineItemUsage.CONTINUE, 3, RoutineTimeSlot.MORNING, 100L),
				new RoutineItemRequest(RoutineItemUsage.DISCONTINUE, null, null, 999L)
		));
	}

	private void recordCheckIn(String token, Long episodeId, LocalDate date, CheckInStatus status) throws Exception {
		mockMvc.perform(put("/api/episodes/" + episodeId + "/check-ins/" + date)
						.header(AUTH, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CheckInRequest(status))))
				.andExpect(status().isOk());
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private String createAnonymousAccountToken() throws Exception {
		String response = mockMvc.perform(post("/api/accounts/anonymous"))
				.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(response);
		return json.get("accessToken").asText();
	}
}
