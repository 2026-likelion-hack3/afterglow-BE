package com.afterglow.tracking.daily.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DailyTrackingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 수면과_컨디션을_기록하면_204를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		DailyTrackingRequest request = new DailyTrackingRequest(
			LocalDate.of(2026, 8, 16),
			com.afterglow.tracking.daily.domain.SleepLevel.POOR,
			com.afterglow.tracking.daily.domain.ConditionLevel.NORMAL,
			37.5665,
			126.9780
		);

		mockMvc.perform(post("/api/tracking/daily")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent());
	}

	@Test
	void 인증_없이_기록하면_401을_받는다() throws Exception {
		DailyTrackingRequest request = new DailyTrackingRequest(
			LocalDate.of(2026, 8, 16),
			com.afterglow.tracking.daily.domain.SleepLevel.POOR,
			com.afterglow.tracking.daily.domain.ConditionLevel.NORMAL,
			37.5665,
			126.9780
		);

		mockMvc.perform(post("/api/tracking/daily")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 같은_날짜에_다시_기록하면_기존_기록을_수정한다() throws Exception {
		String token = createAnonymousAccountToken();

		DailyTrackingRequest firstRequest = new DailyTrackingRequest(
			LocalDate.of(2026, 8, 16),
			com.afterglow.tracking.daily.domain.SleepLevel.POOR,
			com.afterglow.tracking.daily.domain.ConditionLevel.BAD,
			37.5665,
			126.9780
		);

		mockMvc.perform(post("/api/tracking/daily")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(firstRequest)))
			.andExpect(status().isNoContent());

		DailyTrackingRequest secondRequest = new DailyTrackingRequest(
			LocalDate.of(2026, 8, 16),
			com.afterglow.tracking.daily.domain.SleepLevel.WELL,
			com.afterglow.tracking.daily.domain.ConditionLevel.GOOD,
			37.5665,
			126.9780
		);

		mockMvc.perform(post("/api/tracking/daily")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(secondRequest)))
			.andExpect(status().isNoContent());
	}

	@Test
	void 오늘_기록이_있으면_조회할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();

		DailyTrackingRequest request = new DailyTrackingRequest(
			LocalDate.now(),
			com.afterglow.tracking.daily.domain.SleepLevel.WELL,
			com.afterglow.tracking.daily.domain.ConditionLevel.GOOD,
			37.5665,
			126.9780
		);

		mockMvc.perform(post("/api/tracking/daily")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/tracking/daily")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.recordedDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.sleepLevel").value("WELL"))
			.andExpect(jsonPath("$.conditionLevel").value("GOOD"))
			.andExpect(jsonPath("$.temperature").isNumber())
			.andExpect(jsonPath("$.humidity").isNumber())
			.andExpect(jsonPath("$.uvIndex").isNumber());
	}

	@Test
	void 오늘_기록이_없으면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(get("/api/tracking/daily")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isNotFound());
	}

	@Test
	void 인증_없이_오늘_기록을_조회하면_401을_받는다() throws Exception {
		mockMvc.perform(get("/api/tracking/daily"))
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
