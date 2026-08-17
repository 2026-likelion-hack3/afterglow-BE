package com.afterglow.tracking.daily.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.tracking.daily.api.DailyTrackingResponse;
import com.afterglow.tracking.daily.api.WeeklyReportResponse;
import com.afterglow.tracking.daily.domain.DailyTrackingRepository;

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

		List<DailyTrackingResponse> records = dailyTrackingRepository
			.findByAccountIdAndRecordedDateBetween(accountId, from, to)
			.stream()
			.map(DailyTrackingResponse::new)
			.toList();

		return new WeeklyReportResponse(
			from,
			to,
			records.size(),
			from.until(to).getDays() + 1,
			records
		);
	}
}
