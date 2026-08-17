package com.afterglow.notification.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.afterglow.notification.domain.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 루틴_알림을_켜고_설정할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();

		NotificationSettingRequest request = new NotificationSettingRequest(
			NotificationType.ROUTINE,
			true,
			java.time.LocalTime.of(21, 0),
			"Asia/Seoul"
		);

		mockMvc.perform(post("/api/notifications/settings")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent());
	}

	@Test
	void 일상_기록_알림을_끌_수_있다() throws Exception {
		String token = createAnonymousAccountToken();

		NotificationSettingRequest request = new NotificationSettingRequest(
			NotificationType.DAILY_TRACKING,
			false,
			java.time.LocalTime.of(22, 0),
			"Asia/Seoul"
		);

		mockMvc.perform(post("/api/notifications/settings")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNoContent());
	}

	@Test
	void 알림_시간을_변경하면_기존_설정을_수정한다() throws Exception {
		String token = createAnonymousAccountToken();

		NotificationSettingRequest firstRequest = new NotificationSettingRequest(
			NotificationType.ROUTINE,
			true,
			java.time.LocalTime.of(21, 0),
			"Asia/Seoul"
		);

		mockMvc.perform(post("/api/notifications/settings")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(firstRequest)))
			.andExpect(status().isNoContent());

		NotificationSettingRequest secondRequest = new NotificationSettingRequest(
			NotificationType.ROUTINE,
			true,
			java.time.LocalTime.of(22, 30),
			"Asia/Seoul"
		);

		mockMvc.perform(post("/api/notifications/settings")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(secondRequest)))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/notifications/settings")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].type").value("ROUTINE"))
			.andExpect(jsonPath("$[0].enabled").value(true))
			.andExpect(jsonPath("$[0].notificationTime").value("22:30:00"))
			.andExpect(jsonPath("$[0].timezone").value("Asia/Seoul"));
	}

	@Test
	void 두_종류의_알림_설정을_각각_조회할_수_있다() throws Exception {
		String token = createAnonymousAccountToken();

		NotificationSettingRequest routineRequest = new NotificationSettingRequest(
			NotificationType.ROUTINE,
			true,
			java.time.LocalTime.of(21, 0),
			"Asia/Seoul"
		);

		NotificationSettingRequest dailyTrackingRequest = new NotificationSettingRequest(
			NotificationType.DAILY_TRACKING,
			true,
			java.time.LocalTime.of(22, 0),
			"Asia/Seoul"
		);

		mockMvc.perform(post("/api/notifications/settings")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(routineRequest)))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/notifications/settings")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dailyTrackingRequest)))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/notifications/settings")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void 인증_없이_알림_설정을_조회하면_401을_받는다() throws Exception {
		mockMvc.perform(get("/api/notifications/settings"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 인증_없이_알림_설정을_변경하면_401을_받는다() throws Exception {
		NotificationSettingRequest request = new NotificationSettingRequest(
			NotificationType.ROUTINE,
			true,
			java.time.LocalTime.of(21, 0),
			"Asia/Seoul"
		);

		mockMvc.perform(post("/api/notifications/settings")
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
