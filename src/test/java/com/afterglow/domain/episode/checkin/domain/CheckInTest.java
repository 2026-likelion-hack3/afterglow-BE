package com.afterglow.domain.episode.checkin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CheckInTest {

	@Test
	void 생성_직후_전달한_값을_그대로_가진다() {
		CheckIn checkIn = CheckIn.create(1L, LocalDate.of(2026, 8, 17), CheckInStatus.IMPROVED);

		assertThat(checkIn.getEpisodeId()).isEqualTo(1L);
		assertThat(checkIn.getCheckInDate()).isEqualTo(LocalDate.of(2026, 8, 17));
		assertThat(checkIn.getStatus()).isEqualTo(CheckInStatus.IMPROVED);
	}

	@Test
	void overwrite_하면_상태가_최신_값으로_바뀐다() {
		CheckIn checkIn = CheckIn.create(1L, LocalDate.of(2026, 8, 17), CheckInStatus.SAME);

		checkIn.overwrite(CheckInStatus.WORSE);

		assertThat(checkIn.getStatus()).isEqualTo(CheckInStatus.WORSE);
	}
}
