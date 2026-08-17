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

import com.afterglow.tracking.daily.domain.ConditionLevel;
import com.afterglow.tracking.daily.domain.SleepLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WeeklyReportControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 주간_리포트를_조회하면_기록된_날짜와_전체_날짜를_확인할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();

		createDailyTracking(
			token,
			LocalDate.of(2026, 8, 10),
			SleepLevel.WELL,
			ConditionLevel.GOOD
		);

		createDailyTracking(
			token,
			LocalDate.of(2026, 8, 12),
			SleepLevel.NORMAL,
			ConditionLevel.NORMAL
		);

		createDailyTracking(
			token,
			LocalDate.of(2026, 8, 15),
			SleepLevel.POOR,
			ConditionLevel.BAD
		);

		mockMvc.perform(get("/api/tracking/weekly-report")
				.header("Authorization", "Bearer " + token)
				.param("from", "2026-08-10")
				.param("to", "2026-08-16"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.from").value("2026-08-10"))
			.andExpect(jsonPath("$.to").value("2026-08-16"))
			.andExpect(jsonPath("$.recordedDays").value(3))
			.andExpect(jsonPath("$.totalDays").value(7))
			.andExpect(jsonPath("$.records.length()").value(3));
	}

	@Test
	void 기록이_없어도_주간_리포트를_조회할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(get("/api/tracking/weekly-report")
				.header("Authorization", "Bearer " + token)
				.param("from", "2026-08-10")
				.param("to", "2026-08-16"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.recordedDays").value(0))
			.andExpect(jsonPath("$.totalDays").value(7))
			.andExpect(jsonPath("$.records.length()").value(0));
	}

	@Test
	void 인증_없이_주간_리포트를_조회하면_401을_받는다() throws Exception {
		mockMvc.perform(get("/api/tracking/weekly-report")
				.param("from", "2026-08-10")
				.param("to", "2026-08-16"))
			.andExpect(status().isUnauthorized());
	}

	private void createDailyTracking(
		String token,
		LocalDate recordedDate,
		SleepLevel sleepLevel,
		ConditionLevel conditionLevel) throws Exception {

		DailyTrackingRequest request = new DailyTrackingRequest(
			recordedDate,
			sleepLevel,
			conditionLevel,
			37.5665,
			126.9780
		);

		mockMvc.perform(post("/api/tracking/daily")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent());
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
