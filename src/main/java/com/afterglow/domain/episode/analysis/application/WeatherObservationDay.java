package com.afterglow.domain.episode.analysis.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.tracking.daily.application.TrackingQueryResponse;

/**
 * Weather raw 값과 그날 CheckIn 상태를 날짜로 정렬한 값 — threshold/matching 판정은 하지 않는다(기획
 * 답변 대기, 2026-08-20 기준). Tracking raw 조회와 CheckIn 이력을 실제로 연결해 두는 준비 단계까지만이다.
 *
 * @param checkInStatus 그날 CheckIn이 없으면 null — 아직 "일치" 판정을 하지 않으므로 null 허용.
 */
public record WeatherObservationDay(
		LocalDate recordedDate,
		Double temperature,
		Double minTemperature,
		Double humidity,
		Double uvIndex,
		CheckInStatus checkInStatus
) {
	/** Weather raw 목록을 같은 날짜의 CheckIn 상태와 정렬한다(recordedDate 오름차순). CheckIn이 없는 날도 포함한다 — 제외 여부는 threshold가 정해지면 결정한다. */
	public static List<WeatherObservationDay> align(
			List<TrackingQueryResponse> weatherRaw, Map<LocalDate, CheckInStatus> checkInStatuses) {
		return weatherRaw.stream()
				.sorted((a, b) -> a.recordedDate().compareTo(b.recordedDate()))
				.map(day -> new WeatherObservationDay(
						day.recordedDate(), day.temperature(), day.minTemperature(), day.humidity(), day.uvIndex(),
						checkInStatuses.get(day.recordedDate())))
				.toList();
	}

	/** 실제 coverage 계산에 쓸 수 있는 날 — weather raw와 CheckIn이 모두 있는 날짜만. */
	public boolean hasCheckIn() {
		return checkInStatus != null;
	}
}
