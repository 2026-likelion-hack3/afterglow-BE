package com.afterglow.global.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 프론트엔드(Expo Web localhost:8081, 웹 localhost:3000)에서 오는 CORS preflight/실제 요청이
 * Security chain을 올바르게 통과하는지 검증한다. 허용 Origin은 application.yml의
 * afterglow.cors.allowed-origins(comma-separated) 기본값을 그대로 사용한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigurationTest {

	private static final String EXPO_WEB_ORIGIN = "http://localhost:8081";
	private static final String WEB_ORIGIN = "http://localhost:3000";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void Expo_Web_origin_8081의_onboarding_PUT_preflight는_2xx와_CORS_헤더를_받는다() throws Exception {
		assertPreflightAllowed(EXPO_WEB_ORIGIN);
	}

	@Test
	void 웹_origin_3000의_onboarding_PUT_preflight는_2xx와_CORS_헤더를_받는다() throws Exception {
		assertPreflightAllowed(WEB_ORIGIN);
	}

	@Test
	void 허용되지_않은_origin의_preflight는_CORS_헤더_없이_거부된다() throws Exception {
		mockMvc.perform(options("/api/onboarding")
						.header(HttpHeaders.ORIGIN, "http://evil.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void CORS_preflight가_permitAll이더라도_인증_없는_실제_PUT_요청은_여전히_401을_받는다() throws Exception {
		mockMvc.perform(put("/api/onboarding")
						.header(HttpHeaders.ORIGIN, EXPO_WEB_ORIGIN)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"ageRange\":null,\"menstrualStatus\":null}"))
				.andExpect(status().isUnauthorized());
	}

	private void assertPreflightAllowed(String origin) throws Exception {
		ResultActions result = mockMvc.perform(options("/api/onboarding")
				.header(HttpHeaders.ORIGIN, origin)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"));

		result.andExpect(status().is2xxSuccessful())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PUT")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("authorization")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("content-type")));
	}
}
