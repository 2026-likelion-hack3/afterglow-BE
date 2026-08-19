package com.afterglow.domain.vanity.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VanityControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 존재하지_않는_제품을_조회하면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(get("/api/vanity/products/{productId}", 999999L)
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isNotFound());
	}

	private String createAnonymousAccountToken() throws Exception {
		String response = mockMvc.perform(post("/api/accounts/anonymous"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		return objectMapper.readTree(response)
			.get("accessToken")
			.asText();
	}

	@Test
	void 존재하지_않는_제품을_수정하면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		String request = """
                {
                  "name": "테스트 제품",
                  "registrationSource": "MANUAL"
                }
                """;

		mockMvc.perform(patch("/api/vanity/products/{productId}", 999999L)
				.header("Authorization", "Bearer " + token)
				.contentType("application/json")
				.content(request))
			.andExpect(status().isNotFound());
	}

	@Test
	void 존재하지_않는_제품을_삭제하면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(delete("/api/vanity/products/{productId}", 999999L)
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isNotFound());
	}
}
