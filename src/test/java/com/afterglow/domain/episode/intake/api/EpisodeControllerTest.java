package com.afterglow.domain.episode.intake.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.analysis.domain.CandidateType;
import com.afterglow.domain.episode.analysis.domain.CombinationEvidence;
import com.afterglow.domain.episode.analysis.domain.Confidence;
import com.afterglow.domain.episode.analysis.domain.ConflictPlacement;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResult;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResultRepository;
import com.afterglow.domain.episode.analysis.domain.Evidence;
import com.afterglow.domain.episode.analysis.domain.FrequencyEvidence;
import com.afterglow.domain.episode.analysis.domain.TimingEvidence;
import com.afterglow.domain.episode.card.domain.ResultCard;
import com.afterglow.domain.episode.card.domain.ResultCardResult;
import com.afterglow.domain.episode.card.domain.ResultCardType;
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
import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.Product;
import com.afterglow.domain.vanity.RegistrationSource;
import com.afterglow.domain.vanity.UsageTiming;
import com.afterglow.domain.vanity.infrastructure.ProductRepository;
import com.afterglow.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
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
	private EpisodeAnalysisResultRepository episodeAnalysisResultRepository;

	@Autowired
	private ProductRepository productRepository;

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

	// ------------------------------------------------------------------
	// E3(여러 회차 요약, 2026-08-20) — 새 판정/추론 없이 기존 Episode/Analysis/Routine/CheckIn/Vanity
	// Product 데이터를 조합만 한다. "완료" = Routine이 있고, 기존 RoutineService.judgeDay3가 반환한
	// verdict가 WITHHELD(판정 없음, 기존 enum semantics)가 아닌 상태(2026-08-21 정정).
	// ------------------------------------------------------------------

	@Test
	void 완료_Episode가_0건이면_insufficient다() throws Exception {
		String token = createAnonymousAccountToken();
		createEpisode(token); // routine이 없어 완료가 아니다.

		JsonNode body = getMultiSummary(token);
		assertThat(body.get("sufficient").asBoolean()).isFalse();
		assertThat(body.get("completedEpisodeCount").asInt()).isEqualTo(0);
		assertThat(body.get("causeCounts")).isEmpty();
		assertThat(body.get("averageImprovementDay").isNull()).isTrue();
		assertThat(body.get("repeatedProductPatterns")).isEmpty();
		assertThat(body.get("combinationPatterns")).isEmpty();
	}

	/** Routine은 있지만 CheckIn이 하나도 없어 Day3Verdict.WITHHELD(응답 2일 이하, 판정 없음)다 — 완료로 잡히면 안 된다. */
	@Test
	void Day3_verdict가_WITHHELD면_완료로_집계되지_않는다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);
		Long episodeId = createEpisode(token);
		submitIntake(token, episodeId);
		routineRepository.save(Routine.create(episodeId, accountId, LocalDate.now(), List.of()));
		// CheckIn을 전혀 남기지 않는다 — WITHHELD 상태를 그대로 유지.

		JsonNode body = getMultiSummary(token);
		assertThat(body.get("sufficient").asBoolean()).isFalse();
		assertThat(body.get("completedEpisodeCount").asInt()).isEqualTo(0);
	}

	@Test
	void 완료_Episode가_1건이면_insufficient다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);
		completeEpisode(token, accountId, Map.of());

		JsonNode body = getMultiSummary(token);
		assertThat(body.get("sufficient").asBoolean()).isFalse();
		assertThat(body.get("completedEpisodeCount").asInt()).isEqualTo(1);
	}

	@Test
	void 완료_Episode가_2건이면_insufficient다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);
		completeEpisode(token, accountId, Map.of());
		completeEpisode(token, accountId, Map.of());

		JsonNode body = getMultiSummary(token);
		assertThat(body.get("sufficient").asBoolean()).isFalse();
		assertThat(body.get("completedEpisodeCount").asInt()).isEqualTo(2);
	}

	@Test
	void 완료_Episode가_정확히_3건이면_sufficient다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);
		completeEpisode(token, accountId, Map.of());
		completeEpisode(token, accountId, Map.of());
		completeEpisode(token, accountId, Map.of());

		JsonNode body = getMultiSummary(token);
		assertThat(body.get("sufficient").asBoolean()).isTrue();
		assertThat(body.get("completedEpisodeCount").asInt()).isEqualTo(3);
	}

	/** 4건 중 가장 오래된 PRODUCT 원인 episode는 제외되고, 최신 3건(SLEEP/SLEEP/WEATHER)만 causeCounts에 반영돼야 한다. */
	@Test
	void 완료_Episode가_4건_이상이면_최신_3건만_causeCounts에_반영된다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);

		Long e1 = completeEpisode(token, accountId, Map.of());
		saveAnalysisResult(accountId, e1, CandidateType.PRODUCT,
			new TimingEvidence(1L, LocalDate.now().minusDays(10), LocalDate.now()));
		Long e2 = completeEpisode(token, accountId, Map.of());
		saveAnalysisResult(accountId, e2, CandidateType.SLEEP, new FrequencyEvidence(10, 7));
		Long e3 = completeEpisode(token, accountId, Map.of());
		saveAnalysisResult(accountId, e3, CandidateType.SLEEP, new FrequencyEvidence(10, 7));
		Long e4 = completeEpisode(token, accountId, Map.of());
		saveAnalysisResult(accountId, e4, CandidateType.WEATHER, new FrequencyEvidence(10, 7));

		JsonNode body = getMultiSummary(token);
		assertThat(body.get("sufficient").asBoolean()).isTrue();
		assertThat(body.get("completedEpisodeCount").asInt()).isEqualTo(4);

		Map<String, Integer> counts = new HashMap<>();
		body.get("causeCounts").forEach(node -> counts.put(node.get("causeType").asText(), node.get("count").asInt()));
		assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of("SLEEP", 2, "WEATHER", 1));
	}

	@Test
	void Day1_3_중_첫_IMPROVED_dayNumber의_평균을_계산한다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);

		completeEpisode(token, accountId, Map.of(1, CheckInStatus.IMPROVED));
		completeEpisode(token, accountId, Map.of(1, CheckInStatus.SAME, 2, CheckInStatus.IMPROVED));
		completeEpisode(token, accountId, Map.of(1, CheckInStatus.WORSE, 2, CheckInStatus.SAME, 3, CheckInStatus.IMPROVED));

		JsonNode body = getMultiSummary(token);
		assertThat(body.get("averageImprovementDay").asDouble()).isEqualTo(2.0);
	}

	@Test
	void IMPROVED이_하나도_없으면_averageImprovementDay는_null이다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);

		completeEpisode(token, accountId, Map.of(1, CheckInStatus.SAME));
		completeEpisode(token, accountId, Map.of(1, CheckInStatus.WORSE));
		completeEpisode(token, accountId, Map.of());

		JsonNode body = getMultiSummary(token);
		assertThat(body.get("averageImprovementDay").isNull()).isTrue();
	}

	@Test
	void PRODUCT_cause는_제품의_type과_interactionTags를_그대로_반환한다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);

		Long productId = productRepository.save(Product.builder()
			.accountId(accountId)
			.name("테스트 크림")
			.type("크림")
			.usageTiming(UsageTiming.EVENING)
			.interactionTags(Set.of(InteractionTag.RETINOL))
			.registrationSource(RegistrationSource.MANUAL)
			.build()).getId();

		Long e1 = completeEpisode(token, accountId, Map.of());
		saveAnalysisResult(accountId, e1, CandidateType.PRODUCT,
			new TimingEvidence(productId, LocalDate.now().minusDays(10), LocalDate.now()));
		completeEpisode(token, accountId, Map.of());
		completeEpisode(token, accountId, Map.of());

		JsonNode body = getMultiSummary(token);
		JsonNode pattern = body.get("repeatedProductPatterns").get(0);
		assertThat(pattern.get("episodeId").asLong()).isEqualTo(e1);
		assertThat(pattern.get("productId").asLong()).isEqualTo(productId);
		assertThat(pattern.get("product").get("type").asText()).isEqualTo("크림");
		assertThat(pattern.get("product").get("interactionTags").get(0).asText()).isEqualTo("RETINOL");
	}

	@Test
	void COMBINATION_cause는_evidenceTagA_B를_그대로_반환한다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);

		Long e1 = completeEpisode(token, accountId, Map.of());
		saveAnalysisResult(accountId, e1, CandidateType.COMBINATION,
			new CombinationEvidence("RETINOL", "ACID", ConflictPlacement.SAME_TIME_SLOT));
		completeEpisode(token, accountId, Map.of());
		completeEpisode(token, accountId, Map.of());

		JsonNode body = getMultiSummary(token);
		JsonNode pattern = body.get("combinationPatterns").get(0);
		assertThat(pattern.get("episodeId").asLong()).isEqualTo(e1);
		assertThat(pattern.get("tagA").asText()).isEqualTo("RETINOL");
		assertThat(pattern.get("tagB").asText()).isEqualTo("ACID");
	}

	@Test
	void 존재하지_않는_제품을_참조하는_PRODUCT_cause는_product_detail이_null이다() throws Exception {
		String token = createAnonymousAccountToken();
		Long accountId = jwtTokenProvider.parseAccountId(token);

		Long e1 = completeEpisode(token, accountId, Map.of());
		saveAnalysisResult(accountId, e1, CandidateType.PRODUCT,
			new TimingEvidence(999999L, LocalDate.now().minusDays(10), LocalDate.now()));
		completeEpisode(token, accountId, Map.of());
		completeEpisode(token, accountId, Map.of());

		JsonNode body = getMultiSummary(token);
		JsonNode pattern = body.get("repeatedProductPatterns").get(0);
		assertThat(pattern.get("productId").asLong()).isEqualTo(999999L);
		assertThat(pattern.get("product").isNull()).isTrue();
	}

	@Test
	void 다른_계정의_완료_Episode는_집계에_포함되지_않는다() throws Exception {
		String otherToken = createAnonymousAccountToken();
		Long otherAccountId = jwtTokenProvider.parseAccountId(otherToken);
		completeEpisode(otherToken, otherAccountId, Map.of());
		completeEpisode(otherToken, otherAccountId, Map.of());
		completeEpisode(otherToken, otherAccountId, Map.of());

		String token = createAnonymousAccountToken();

		JsonNode body = getMultiSummary(token);
		assertThat(body.get("sufficient").asBoolean()).isFalse();
		assertThat(body.get("completedEpisodeCount").asInt()).isEqualTo(0);
	}

	@Test
	void 인증_없이_multi_summary를_조회하면_401을_받는다() throws Exception {
		mockMvc.perform(get("/api/episodes/multi-summary"))
			.andExpect(status().isUnauthorized());
	}

	private JsonNode getMultiSummary(String token) throws Exception {
		String response = mockMvc.perform(get("/api/episodes/multi-summary").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
		return objectMapper.readTree(response);
	}

	/**
	 * Episode 생성+문진+Routine(빈 items) 저장까지 마쳐 "완료" 후보로 만든다. 명시하지 않은 날은 SAME으로
	 * 채운다 — Day3JudgmentEngine 확정 규칙상 나빠짐(WORSE) 없이 응답이 2일 이하면 WITHHELD(판정 없음)가
	 * 나와 "완료"(WITHHELD가 아닌 verdict)로 잡히지 않기 때문이다. SAME으로 채워도 firstImprovedDay
	 * 계산에는 영향이 없다(IMPROVED만 찾으므로).
	 */
	private Long completeEpisode(String token, Long accountId, Map<Integer, CheckInStatus> dayStatuses) throws Exception {
		Long episodeId = createEpisode(token);
		submitIntake(token, episodeId);

		LocalDate startDate = LocalDate.now();
		routineRepository.save(Routine.create(episodeId, accountId, startDate, List.of()));

		for (int day = 1; day <= 3; day++) {
			CheckInStatus status = dayStatuses.getOrDefault(day, CheckInStatus.SAME);
			checkInRepository.save(CheckIn.create(episodeId, startDate.plusDays(day - 1), status));
		}

		return episodeId;
	}

	private void saveAnalysisResult(Long accountId, Long episodeId, CandidateType causeType, Evidence evidence) {
		ResultCard card = new ResultCard(ResultCardType.DISCONTINUE, causeType, evidence, null);
		ResultCardResult result = ResultCardResult.determined(Confidence.HIGH, List.of(card));
		episodeAnalysisResultRepository.save(EpisodeAnalysisResult.from(episodeId, accountId, result));
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
