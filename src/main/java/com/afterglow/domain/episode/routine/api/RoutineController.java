package com.afterglow.domain.episode.routine.api;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.domain.episode.checkin.domain.Day3JudgmentResult;
import com.afterglow.domain.episode.routine.application.RoutineService;
import com.afterglow.domain.episode.routine.domain.Routine;
import com.afterglow.domain.episode.routine.domain.RoutineCreateInput;
import com.afterglow.domain.episode.routine.domain.RoutineItemInput;
import com.afterglow.global.security.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 기능명세서 3.2(Manyfast F-VUQBTM) — 명세에 URL/request/response 계약이 명시돼 있지 않아, 기존
 * {@code /api/episodes/{episodeId}/...} convention(예: {@link com.afterglow.domain.episode.checkin.api.CheckInController})을
 * 그대로 따라 마감에 필요한 최소 API만 구현했다(보고서에 명시).
 */
@RestController
@RequestMapping("/api/episodes/{episodeId}/routine")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class RoutineController {

	private final RoutineService routineService;

	@PostMapping
	public ResponseEntity<RoutineResponse> start(
			@AuthenticationPrincipal Long accountId, @PathVariable Long episodeId,
			@Valid @RequestBody RoutineCreateRequest request) {
		Routine routine = routineService.start(accountId, episodeId, toInput(request));
		return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(routine));
	}

	@GetMapping
	public ResponseEntity<RoutineResponse> get(
			@AuthenticationPrincipal Long accountId, @PathVariable Long episodeId) {
		Routine routine = routineService.get(accountId, episodeId);
		return ResponseEntity.ok(toResponse(routine));
	}

	@GetMapping("/day3-result")
	public ResponseEntity<Day3ResultResponse> getDay3Result(
			@AuthenticationPrincipal Long accountId, @PathVariable Long episodeId) {
		Day3JudgmentResult result = routineService.judgeDay3(accountId, episodeId);
		return ResponseEntity.ok(new Day3ResultResponse(result.verdict()));
	}

	private RoutineCreateInput toInput(RoutineCreateRequest request) {
		List<RoutineItemInput> items = request.items().stream()
				.map(item -> new RoutineItemInput(item.usage(), item.dayNumber(), item.timeSlot(), item.productId()))
				.toList();
		return new RoutineCreateInput(items);
	}

	private RoutineResponse toResponse(Routine routine) {
		List<RoutineDayResponse> days = IntStream.rangeClosed(1, Routine.DURATION_DAYS)
				.mapToObj(dayNumber -> new RoutineDayResponse(
						dayNumber,
						routine.dateOf(dayNumber),
						routine.continueItemsOf(dayNumber).stream()
								.map(item -> new RoutineItemResponse(item.getTimeSlot(), item.getProductId()))
								.toList()
				))
				.toList();

		return new RoutineResponse(
				routine.getId(),
				routine.getStartDate(),
				routine.getStatus(),
				days,
				routine.discontinuedProductIds()
		);
	}
}
