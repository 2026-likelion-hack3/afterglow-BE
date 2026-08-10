package com.afterglow.account.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.account.domain.VerificationPurpose;
import com.afterglow.account.infrastructure.EmailVerificationJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private EmailVerificationJpaRepository emailVerificationJpaRepository;

	@Test
	void 익명_계정을_생성하면_토큰을_받는다() throws Exception {
		mockMvc.perform(post("/api/accounts/anonymous"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void 인증_없이_회원가입_인증코드를_요청하면_401을_받는다() throws Exception {
		mockMvc.perform(post("/api/accounts/me/email/verification-codes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new EmailRequest(uniqueEmail()))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 회원가입_인증코드를_올바르게_검증하면_이메일이_연결된_토큰을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		String email = uniqueEmail();

		requestSignupCode(token, email);
		String code = latestCode(email, VerificationPurpose.SIGNUP);

		mockMvc.perform(post("/api/accounts/me/email/verification")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new EmailVerifyRequest(email, code))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void 잘못된_인증코드로_검증하면_400을_받는다() throws Exception {
		String token = createAnonymousAccountToken();
		String email = uniqueEmail();

		requestSignupCode(token, email);

		mockMvc.perform(post("/api/accounts/me/email/verification")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new EmailVerifyRequest(email, "000000"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"));
	}

	@Test
	void 이미_인증된_이메일로_다시_가입을_시도하면_409를_받는다() throws Exception {
		String email = uniqueEmail();
		verifySignup(createAnonymousAccountToken(), email);

		String secondToken = createAnonymousAccountToken();

		mockMvc.perform(post("/api/accounts/me/email/verification-codes")
						.header("Authorization", "Bearer " + secondToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new EmailRequest(email))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}

	@Test
	void 가입된_이메일로_로그인하면_인증코드_검증_후_토큰을_받는다() throws Exception {
		String email = uniqueEmail();
		verifySignup(createAnonymousAccountToken(), email);

		mockMvc.perform(post("/api/accounts/login/verification-codes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new EmailRequest(email))))
				.andExpect(status().isNoContent());

		String code = latestCode(email, VerificationPurpose.LOGIN);

		mockMvc.perform(post("/api/accounts/login/verification")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new EmailVerifyRequest(email, code))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void 등록되지_않은_이메일로_로그인_코드를_요청하면_404를_받는다() throws Exception {
		mockMvc.perform(post("/api/accounts/login/verification-codes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new EmailRequest(uniqueEmail()))))
				.andExpect(status().isNotFound());
	}

	private String createAnonymousAccountToken() throws Exception {
		String response = mockMvc.perform(post("/api/accounts/anonymous"))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("accessToken").asText();
	}

	private void requestSignupCode(String token, String email) throws Exception {
		mockMvc.perform(post("/api/accounts/me/email/verification-codes")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new EmailRequest(email))))
				.andExpect(status().isNoContent());
	}

	private void verifySignup(String token, String email) throws Exception {
		requestSignupCode(token, email);
		String code = latestCode(email, VerificationPurpose.SIGNUP);
		mockMvc.perform(post("/api/accounts/me/email/verification")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new EmailVerifyRequest(email, code))))
				.andExpect(status().isOk());
	}

	private String latestCode(String email, VerificationPurpose purpose) {
		return emailVerificationJpaRepository.findAll().stream()
				.filter(v -> v.getEmail().equals(email) && v.getPurpose() == purpose)
				.findFirst()
				.map(v -> v.getCode())
				.orElseThrow();
	}

	private String uniqueEmail() {
		return "test-" + UUID.randomUUID() + "@example.com";
	}
}
