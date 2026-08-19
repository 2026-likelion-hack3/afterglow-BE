package com.afterglow.domain.tracking.daily.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.tracking.daily.api.DailyTrackingResponse;
import com.afterglow.domain.tracking.daily.api.WeeklyReportResponse;
import com.afterglow.domain.tracking.daily.domain.DailyTrackingRepository;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportService {

	private final DailyTrackingRepository dailyTrackingRepository;

	public WeeklyReportResponse getWeeklyReport(
		Long accountId,
		LocalDate from,
		LocalDate to) {

		if (from.isAfter(to)) {
			throw new AfterglowException(
				ErrorCode.INVALID_REQUEST,
				"시작일은 종료일보다 늦을 수 없습니다."
			);
		}

		long totalDays = ChronoUnit.DAYS.between(from, to) + 1;

		if (totalDays > 7) {
			throw new AfterglowException(
				ErrorCode.INVALID_REQUEST,
				"주간 리포트 조회 범위는 최대 7일입니다."
			);
		}

		List<DailyTrackingResponse> records = dailyTrackingRepository
			.findByAccountIdAndRecordedDateBetween(accountId, from, to)
			.stream()
			.map(DailyTrackingResponse::new)
			.toList();

		return new WeeklyReportResponse(
			from,
			to,
			records.size(),
			Math.toIntExact(totalDays),
			records
		);
	}
}
