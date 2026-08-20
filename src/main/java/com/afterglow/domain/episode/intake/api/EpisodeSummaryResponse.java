package com.afterglow.domain.episode.intake.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.afterglow.domain.episode.analysis.domain.CandidateType;
import com.afterglow.domain.episode.analysis.domain.Confidence;
import com.afterglow.domain.episode.card.domain.ResultCardHoldReason;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.checkin.domain.Day3Verdict;
import com.afterglow.domain.episode.domain.EpisodeStatus;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.routine.domain.RoutineStatus;

/**
 * Figma E1(기록 목록) 최소 조회 응답 — 새 판정/집계 없이 기존 Episode/EpisodeAnalysisResult/Routine/
 * CheckIn 데이터를 그대로 조회·조합한다({@code EpisodeSummaryService} 참고).
 *
 * <p><b>"완료/진행중" 통합 상태를 만들지 않았다(2026-08-20)</b> — {@link EpisodeStatus}는
 * SYMPTOM_SELECTED/INTAKE_COMPLETED/ANALYZED까지만 있고 {@link RoutineStatus}도 ACTIVE 하나뿐이라,
 * 이 raw 상태들을 "완료" 대 "진행중" 하나로 합치는 기준이 Manyfast/코드 어디에도 없다 — 임의로 새 규칙을
 * 만들지 않고 raw 값 그대로 반환한다.
 *
 * @param analysisHold        분석이 보류 상태인지(분석 결과가 아직 없으면 null)
 * @param analysisCauseType   원인 후보 타입(보류/미분석이면 null)
 * @param analysisHoldReason  보류 사유(보류가 아니거나 미분석이면 null)
 * @param routineStatus       Routine 상태(아직 시작 전이면 null)
 * @param day1Status          Day1 CheckIn 상태(Routine 없거나 그날 기록이 없으면 null)
 * @param day3Verdict         기존 {@code RoutineService.judgeDay3}를 그대로 재사용한 값(Routine 없으면 null)
 * @param productIds          Routine에 연결된 제품 id(중복 제거, Routine 없으면 빈 배열)
 */
public record EpisodeSummaryResponse(
		Long episodeId,
		LocalDateTime createdAt,
		PrimarySymptom primarySymptom,
		EpisodeStatus status,
		Boolean analysisHold,
		CandidateType analysisCauseType,
		ResultCardHoldReason analysisHoldReason,
		Confidence analysisConfidence,
		RoutineStatus routineStatus,
		LocalDate routineStartDate,
		CheckInStatus day1Status,
		CheckInStatus day2Status,
		CheckInStatus day3Status,
		Day3Verdict day3Verdict,
		List<Long> productIds
) {
}
