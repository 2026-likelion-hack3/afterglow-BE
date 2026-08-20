package com.afterglow.domain.vanity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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

import com.afterglow.domain.vanity.CombinationRule;
import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.application.ProductRegistrationDraft;
import com.afterglow.domain.vanity.application.ProductRegistrationDraftGenerator;
import com.afterglow.domain.vanity.infrastructure.CombinationRuleRepository;
import com.afterglow.domain.vanity.infrastructure.VisionOcrClient;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VanityControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CombinationRuleRepository combinationRuleRepository;

	@MockBean
	private VisionOcrClient visionOcrClient;

	@MockBean
	private ProductRegistrationDraftGenerator productRegistrationDraftGenerator;

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

	@Test
	void OCR_텍스트_구조화에_성공하면_draft와_결정적으로_매칭된_interactionTags를_반환한다() throws Exception {
		String token = createAnonymousAccountToken();

		when(productRegistrationDraftGenerator.generate(any(String.class)))
			.thenReturn(new ProductRegistrationDraft(
				"촉촉 크림", "글로우브랜드", "크림", "정제수, 레티놀, 살리실릭애씨드"));

		String request = """
                {
                  "rawText": "촉촉 크림 글로우브랜드 정제수, 레티놀, 살리실릭애씨드"
                }
                """;

		mockMvc.perform(post("/api/vanity/products/ocr/structure")
				.header("Authorization", "Bearer " + token)
				.contentType("application/json")
				.content(request))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("촉촉 크림"))
			.andExpect(jsonPath("$.brand").value("글로우브랜드"))
			.andExpect(jsonPath("$.type").value("크림"))
			.andExpect(jsonPath("$.keyIngredients").value("정제수, 레티놀, 살리실릭애씨드"))
			.andExpect(jsonPath("$.interactionTags", containsInAnyOrder("RETINOL", "ACID")));
	}

	@Test
	void 매칭_근거가_없는_keyIngredients면_interactionTags는_빈_배열이다() throws Exception {
		String token = createAnonymousAccountToken();

		when(productRegistrationDraftGenerator.generate(any(String.class)))
			.thenReturn(new ProductRegistrationDraft(
				"순한 로션", null, null, "정제수, 글리세린, 나이아신아마이드"));

		String request = """
                {
                  "rawText": "순한 로션 정제수, 글리세린, 나이아신아마이드"
                }
                """;

		mockMvc.perform(post("/api/vanity/products/ocr/structure")
				.header("Authorization", "Bearer " + token)
				.contentType("application/json")
				.content(request))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.interactionTags").isEmpty());
	}

	@Test
	void blank_rawText로_구조화를_요청하면_400을_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		String request = """
                {
                  "rawText": "   "
                }
                """;

		mockMvc.perform(post("/api/vanity/products/ocr/structure")
				.header("Authorization", "Bearer " + token)
				.contentType("application/json")
				.content(request))
			.andExpect(status().isBadRequest());
	}

	@Test
	void generator가_실패하면_500이나_빈_결과가_아니라_503을_받는다() throws Exception {
		String token = createAnonymousAccountToken();

		when(productRegistrationDraftGenerator.generate(any(String.class)))
			.thenThrow(new AfterglowException(ErrorCode.AI_REQUEST_FAILED));

		String request = """
                {
                  "rawText": "정제수, 글리세린"
                }
                """;

		mockMvc.perform(post("/api/vanity/products/ocr/structure")
				.header("Authorization", "Bearer " + token)
				.contentType("application/json")
				.content(request))
			.andExpect(status().isServiceUnavailable());
	}

	@Test
	void OCR_구조화_등록까지_전체_통합_흐름이_functionTags_없이도_성공한다() throws Exception {
		String token = createAnonymousAccountToken();

		// 1. POST /api/vanity/products/ocr → rawText
		MockMultipartFile image = new MockMultipartFile(
			"image",
			"ingredients.jpg",
			"image/jpeg",
			"fake-image".getBytes()
		);

		when(visionOcrClient.extractText(any(byte[].class)))
			.thenReturn("촉촉 크림 글로우브랜드 정제수, 레티놀, 살리실릭애씨드");

		String ocrResponse = mockMvc.perform(multipart("/api/vanity/products/ocr")
				.file(image)
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

		String rawText = objectMapper.readTree(ocrResponse).get("rawText").asText();
		assertThat(rawText).isEqualTo("촉촉 크림 글로우브랜드 정제수, 레티놀, 살리실릭애씨드");

		// 2. POST /api/vanity/products/ocr/structure → structured draft + interactionTags
		when(productRegistrationDraftGenerator.generate(any(String.class)))
			.thenReturn(new ProductRegistrationDraft("촉촉 크림", "글로우브랜드", "크림", "정제수, 레티놀, 살리실릭애씨드"));

		String structureRequest = objectMapper.writeValueAsString(java.util.Map.of("rawText", rawText));

		String structureResponse = mockMvc.perform(post("/api/vanity/products/ocr/structure")
				.header("Authorization", "Bearer " + token)
				.contentType("application/json")
				.content(structureRequest))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

		var draft = objectMapper.readTree(structureResponse);
		java.util.List<String> interactionTags = new java.util.ArrayList<>();
		draft.get("interactionTags").forEach(node -> interactionTags.add(node.asText()));
		assertThat(interactionTags).containsExactlyInAnyOrder("RETINOL", "ACID");

		// 3. draft 필드 + 사용자 입력(openingPeriod/usageTiming)을 합쳐 기존 등록 API 호출.
		// functionTags는 의도적으로 아예 넣지 않는다 — 등록 화면에서 사진으로 알 수 없는 값만 사용자가
		// 채우고, functionTags처럼 이번 자동 구조화 범위 밖인 필드는 아무 값도 보내지 않는 시나리오를
		// 검증한다.
		java.util.Map<String, Object> createRequestBody = new java.util.LinkedHashMap<>();
		createRequestBody.put("name", draft.get("name").asText());
		createRequestBody.put("brand", draft.get("brand").asText());
		createRequestBody.put("type", draft.get("type").asText());
		createRequestBody.put("keyIngredients", draft.get("keyIngredients").asText());
		createRequestBody.put("interactionTags", interactionTags);
		createRequestBody.put("openingPeriod", "RECENT");
		createRequestBody.put("usageTiming", "MORNING");
		createRequestBody.put("registrationSource", "PHOTO");
		// functionTags 키 자체를 넣지 않음.

		String createRequest = objectMapper.writeValueAsString(createRequestBody);

		mockMvc.perform(post("/api/vanity/products")
				.header("Authorization", "Bearer " + token)
				.contentType("application/json")
				.content(createRequest))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.product.name").value("촉촉 크림"))
			.andExpect(jsonPath("$.product.brand").value("글로우브랜드"))
			.andExpect(jsonPath("$.product.type").value("크림"))
			.andExpect(jsonPath("$.product.keyIngredients").value("정제수, 레티놀, 살리실릭애씨드"))
			.andExpect(jsonPath("$.product.interactionTags", containsInAnyOrder("RETINOL", "ACID")))
			.andExpect(jsonPath("$.product.usageTiming").value("MORNING"))
			.andExpect(jsonPath("$.product.functionTags").isEmpty());
	}

	@Test
	void 인증_없이_구조화를_요청하면_401을_받는다() throws Exception {
		String request = """
                {
                  "rawText": "정제수, 글리세린"
                }
                """;

		mockMvc.perform(post("/api/vanity/products/ocr/structure")
				.contentType("application/json")
				.content(request))
			.andExpect(status().isUnauthorized());
	}

	// ------------------------------------------------------------------
	// GET 목록/상세 openingPeriod(2026-08-20, Figma F1/F2 audit 반영, additive) — top-level shape는
	// 기존 그대로(배열/단일 ProductResponse)다. 프론트 호환성 때문에 warnings를 여기 합치지 않고 별도
	// endpoint(GET /api/vanity/products/warnings)로 분리했다(아래 별도 섹션).
	// ------------------------------------------------------------------

	@Test
	void 목록_조회_응답은_기존과_같은_배열_shape를_유지하며_openingPeriod를_포함한다() throws Exception {
		String token = createAnonymousAccountToken();
		Long productId = createProduct(token, "RECENT", "MORNING", java.util.List.of());

		mockMvc.perform(get("/api/vanity/products").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(productId))
			.andExpect(jsonPath("$[0].openingPeriod").value("RECENT"));
	}

	@Test
	void 상세_조회_응답은_기존과_같은_단일_객체_shape를_유지하며_openingPeriod를_포함한다() throws Exception {
		String token = createAnonymousAccountToken();
		Long productId = createProduct(token, "SIX_MONTHS_OR_MORE", "EVENING", java.util.List.of());

		mockMvc.perform(get("/api/vanity/products/{productId}", productId).header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(productId))
			.andExpect(jsonPath("$.openingPeriod").value("SIX_MONTHS_OR_MORE"));
	}

	// ------------------------------------------------------------------
	// GET /api/vanity/products/warnings(2026-08-20) — 기존 products GET shape를 바꾸지 않기 위해 분리한
	// 별도 endpoint. 기존 POST 생성 흐름과 동일한 CombinationWarningService.checkWarnings를 그대로
	// 재사용한다(새 warning 판정 규칙 없음). 계정 전체 기준이라 제품별 매핑은 하지 않는다(known mismatch).
	// ------------------------------------------------------------------

	@Test
	void 조합_경고가_있으면_warnings_endpoint에_포함된다() throws Exception {
		String token = createAnonymousAccountToken();
		seedRetinolAcidRule();
		createProduct(token, "RECENT", "EVENING", java.util.List.of("RETINOL"));
		createProduct(token, "RECENT", "EVENING", java.util.List.of("ACID"));

		mockMvc.perform(get("/api/vanity/products/warnings").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", containsInAnyOrder("레티놀과 산 성분을 같은 시간대에 사용하지 마세요.")));
	}

	@Test
	void 조합_경고가_없으면_warnings_endpoint는_빈_배열을_반환한다() throws Exception {
		String token = createAnonymousAccountToken();
		seedRetinolAcidRule();
		// 같은 아침 시간대에 RETINOL 혼자뿐이라 충돌 조건(같은 시간대에 RETINOL+ACID 함께)을 만족하지 않는다.
		createProduct(token, "RECENT", "MORNING", java.util.List.of("RETINOL"));

		mockMvc.perform(get("/api/vanity/products/warnings").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void 인증_없이_warnings를_조회하면_401을_받는다() throws Exception {
		mockMvc.perform(get("/api/vanity/products/warnings"))
			.andExpect(status().isUnauthorized());
	}

	private void seedRetinolAcidRule() {
		combinationRuleRepository.save(CombinationRule.builder()
			.tagA(InteractionTag.RETINOL)
			.tagB(InteractionTag.ACID)
			.minCount(1)
			.warningMessage("레티놀과 산 성분을 같은 시간대에 사용하지 마세요.")
			.build());
	}

	private Long createProduct(
		String token, String openingPeriod, String usageTiming, java.util.List<String> interactionTags) throws Exception {

		java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
		body.put("name", "테스트 제품");
		body.put("openingPeriod", openingPeriod);
		body.put("usageTiming", usageTiming);
		body.put("interactionTags", interactionTags);
		body.put("registrationSource", "MANUAL");

		String response = mockMvc.perform(post("/api/vanity/products")
				.header("Authorization", "Bearer " + token)
				.contentType("application/json")
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

		return objectMapper.readTree(response).get("product").get("id").asLong();
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
