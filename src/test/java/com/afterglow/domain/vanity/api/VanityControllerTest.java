package com.afterglow.domain.vanity.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.vanity.infrastructure.VisionOcrClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VanityControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private VisionOcrClient visionOcrClient;

	@Test
	void 존재하지_않는_제품을_조회하면_404를_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		mockMvc.perform(get("/api/vanity/products/{productId}", 999999L)
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isNotFound());
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

	@Test
	void 이미지_OCR에_성공하면_raw_text를_반환한다() throws Exception {
		String token = createAnonymousAccountToken();

		MockMultipartFile image = new MockMultipartFile(
			"image",
			"ingredients.jpg",
			"image/jpeg",
			"fake-image".getBytes()
		);

		when(visionOcrClient.extractText(any(byte[].class)))
			.thenReturn("정제수, 글리세린, 나이아신아마이드");

		mockMvc.perform(multipart("/api/vanity/products/ocr")
				.file(image)
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rawText")
				.value("정제수, 글리세린, 나이아신아마이드"));
	}

	@Test
	void Vision_OCR이_실패해도_500이_아닌_빈_raw_text를_반환한다() throws Exception {
		String token = createAnonymousAccountToken();

		MockMultipartFile image = new MockMultipartFile(
			"image",
			"ingredients.jpg",
			"image/jpeg",
			"fake-image".getBytes()
		);

		when(visionOcrClient.extractText(any(byte[].class)))
			.thenThrow(new RuntimeException("Vision API 호출 실패"));

		mockMvc.perform(multipart("/api/vanity/products/ocr")
				.file(image)
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rawText").value(""));
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
}
