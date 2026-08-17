package com.afterglow.domain.episode.checkin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.episode.checkin.domain.CheckIn;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.checkin.infrastructure.CheckInJpaRepository;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.domain.Symptom;

/**
 * CheckInService.record는 find-or-create 후 dirty checking으로 반영되는 부분이 있어, EpisodeServiceTest와
 * 동일한 이유로 테스트 레벨 @Transactional을 붙이지 않는다 — 각 호출/재조회가 독립된 트랜잭션(=독립된
 * persistence context)에서 실행돼야 재조회 결과가 실제 DB 상태를 그대로 반영한다.
 */
@SpringBootTest
class CheckInServiceTest {

	@Autowired
	private CheckInService checkInService;

	@Autowired
	private CheckInRepository checkInRepository;

	@Autowired
	private CheckInJpaRepository checkInJpaRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private EpisodeRepository episodeRepository;

	@Test
	void 기록한_내용은_새_트랜잭션의_재조회에서도_유지된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		LocalDate date = LocalDate.of(2026, 8, 17);

		checkInService.record(accountId, episodeId, date, CheckInStatus.IMPROVED);

		CheckIn reloaded = checkInRepository.findByEpisodeIdAndCheckInDate(episodeId, date).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(CheckInStatus.IMPROVED);
	}

	@Test
	void 같은_날짜에_재기록하면_최신_응답으로_덮어쓰고_row는_하나만_유지된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		LocalDate date = LocalDate.of(2026, 8, 17);

		checkInService.record(accountId, episodeId, date, CheckInStatus.SAME);
		checkInService.record(accountId, episodeId, date, CheckInStatus.WORSE);

		List<CheckIn> all = checkInRepository.findByEpisodeIdOrderByCheckInDateAsc(episodeId);
		assertThat(all).hasSize(1);
		assertThat(all.get(0).getStatus()).isEqualTo(CheckInStatus.WORSE);
	}

	@Test
	void 같은_에피소드의_다른_날짜는_별도로_저장된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();

		checkInService.record(accountId, episodeId, LocalDate.of(2026, 8, 17), CheckInStatus.SAME);
		checkInService.record(accountId, episodeId, LocalDate.of(2026, 8, 18), CheckInStatus.IMPROVED);

		List<CheckIn> all = checkInRepository.findByEpisodeIdOrderByCheckInDateAsc(episodeId);
		assertThat(all).hasSize(2);
		assertThat(all.get(0).getCheckInDate()).isEqualTo(LocalDate.of(2026, 8, 17));
		assertThat(all.get(1).getCheckInDate()).isEqualTo(LocalDate.of(2026, 8, 18));
	}

	@Test
	void DB_unique_constraint가_동일_episode_동일_날짜_중복_저장을_막는다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		LocalDate date = LocalDate.of(2026, 8, 17);

		checkInJpaRepository.saveAndFlush(CheckIn.create(episodeId, date, CheckInStatus.IMPROVED));

		assertThatThrownBy(() ->
				checkInJpaRepository.saveAndFlush(CheckIn.create(episodeId, date, CheckInStatus.SAME)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private Symptom symptom() {
		return Symptom.create(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE);
	}
}
