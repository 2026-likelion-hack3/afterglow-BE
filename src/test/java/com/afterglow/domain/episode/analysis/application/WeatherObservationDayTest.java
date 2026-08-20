package com.afterglow.domain.episode.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.tracking.daily.application.TrackingQueryResponse;
import com.afterglow.domain.tracking.daily.domain.SleepLevel;

class WeatherObservationDayTest {

	@Test
	void raw와_CheckIn을_날짜로_정렬해서_연결한다() {
		LocalDate day1 = LocalDate.of(2026, 8, 1);
		LocalDate day2 = LocalDate.of(2026, 8, 2);
		List<TrackingQueryResponse> raw = List.of(
				new TrackingQueryResponse(day2, SleepLevel.WELL, 28.0, 22.0, 60.0, 8.0),
				new TrackingQueryResponse(day1, SleepLevel.WELL, 27.0, 20.0, 55.0, 7.0));
		Map<LocalDate, CheckInStatus> checkIns = Map.of(day1, CheckInStatus.WORSE);

		List<WeatherObservationDay> aligned = WeatherObservationDay.align(raw, checkIns);

		assertThat(aligned).hasSize(2);
		assertThat(aligned.get(0).recordedDate()).isEqualTo(day1);
		assertThat(aligned.get(0).checkInStatus()).isEqualTo(CheckInStatus.WORSE);
		assertThat(aligned.get(0).hasCheckIn()).isTrue();
		assertThat(aligned.get(1).recordedDate()).isEqualTo(day2);
		assertThat(aligned.get(1).checkInStatus()).isNull();
		assertThat(aligned.get(1).hasCheckIn()).isFalse();
	}

	@Test
	void raw_값들이_그대로_보존된다() {
		LocalDate day = LocalDate.of(2026, 8, 1);
		TrackingQueryResponse raw = new TrackingQueryResponse(day, SleepLevel.POOR, 30.0, 24.0, 70.0, 9.5);

		List<WeatherObservationDay> aligned = WeatherObservationDay.align(List.of(raw), Map.of());

		WeatherObservationDay result = aligned.get(0);
		assertThat(result.temperature()).isEqualTo(30.0);
		assertThat(result.minTemperature()).isEqualTo(24.0);
		assertThat(result.humidity()).isEqualTo(70.0);
		assertThat(result.uvIndex()).isEqualTo(9.5);
	}
}
