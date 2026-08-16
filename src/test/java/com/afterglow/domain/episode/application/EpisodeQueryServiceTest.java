package com.afterglow.domain.episode.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.domain.Symptom;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class EpisodeQueryServiceTest {

	private static final LocalDate FROM = LocalDate.of(2026, 8, 10);
	private static final LocalDate TO = LocalDate.of(2026, 8, 16);

	@Autowired
	private EpisodeQueryService episodeQueryService;

	@Autowired
	private EpisodeRepository episodeRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 기간_내_에피소드_여러_건을_생성일시_오름차순으로_반환한다() {
		Long accountId = 1001L;
		Long laterId = saveEpisodeAt(accountId, LocalDateTime.of(2026, 8, 14, 15, 30), PrimarySymptom.REDNESS, Severity.SEVERE);
		Long earlierId = saveEpisodeAt(accountId, LocalDateTime.of(2026, 8, 11, 9, 0), PrimarySymptom.ITCHING, Severity.MILD);

		List<EpisodeSymptomRecord> result = episodeQueryService.findSymptomsByPeriod(accountId, FROM, TO);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).episodeId()).isEqualTo(earlierId);
		assertThat(result.get(0).createdAt()).isEqualTo(LocalDateTime.of(2026, 8, 11, 9, 0));
		assertThat(result.get(0).primarySymptom()).isEqualTo(PrimarySymptom.ITCHING);
		assertThat(result.get(0).severity()).isEqualTo(Severity.MILD);
		assertThat(result.get(1).episodeId()).isEqualTo(laterId);
	}

	@Test
	void 조회_기간_밖의_에피소드는_제외한다() {
		Long accountId = 1002L;
		saveEpisodeAt(accountId, LocalDateTime.of(2026, 8, 9, 23, 59), PrimarySymptom.REDNESS, Severity.MODERATE);

		List<EpisodeSymptomRecord> result = episodeQueryService.findSymptomsByPeriod(accountId, FROM, TO);

		assertThat(result).isEmpty();
	}

	@Test
	void 다른_계정의_에피소드는_제외한다() {
		Long accountId = 1003L;
		Long otherAccountId = 1004L;
		saveEpisodeAt(otherAccountId, LocalDateTime.of(2026, 8, 12, 10, 0), PrimarySymptom.REDNESS, Severity.MODERATE);

		List<EpisodeSymptomRecord> result = episodeQueryService.findSymptomsByPeriod(accountId, FROM, TO);

		assertThat(result).isEmpty();
	}

	@Test
	void 시작일_00시_정각_데이터는_포함한다() {
		Long accountId = 1005L;
		Long id = saveEpisodeAt(accountId, FROM.atStartOfDay(), PrimarySymptom.REDNESS, Severity.MODERATE);

		List<EpisodeSymptomRecord> result = episodeQueryService.findSymptomsByPeriod(accountId, FROM, TO);

		assertThat(result).extracting(EpisodeSymptomRecord::episodeId).containsExactly(id);
	}

	@Test
	void 종료일_당일_데이터는_포함한다() {
		Long accountId = 1006L;
		Long id = saveEpisodeAt(accountId, LocalDateTime.of(2026, 8, 16, 23, 59, 59), PrimarySymptom.REDNESS, Severity.MODERATE);

		List<EpisodeSymptomRecord> result = episodeQueryService.findSymptomsByPeriod(accountId, FROM, TO);

		assertThat(result).extracting(EpisodeSymptomRecord::episodeId).containsExactly(id);
	}

	@Test
	void 종료일_다음날_00시_데이터는_제외한다() {
		Long accountId = 1007L;
		saveEpisodeAt(accountId, TO.plusDays(1).atStartOfDay(), PrimarySymptom.REDNESS, Severity.MODERATE);

		List<EpisodeSymptomRecord> result = episodeQueryService.findSymptomsByPeriod(accountId, FROM, TO);

		assertThat(result).isEmpty();
	}

	@Test
	void 해당_기간에_에피소드가_없으면_빈_리스트를_반환한다() {
		Long accountId = 1008L;

		List<EpisodeSymptomRecord> result = episodeQueryService.findSymptomsByPeriod(accountId, FROM, TO);

		assertThat(result).isEmpty();
	}

	@Test
	void 시작일이_종료일보다_늦으면_예외가_발생한다() {
		Long accountId = 1009L;

		assertThatThrownBy(() -> episodeQueryService.findSymptomsByPeriod(accountId, TO, FROM))
				.isInstanceOf(AfterglowException.class)
				.satisfies(e -> assertThat(((AfterglowException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
	}

	/** createdAt은 @CreatedDate가 저장 시점에 자동으로 채우므로, 경계값 테스트를 위해 저장 후 직접 덮어쓴다. */
	private Long saveEpisodeAt(Long accountId, LocalDateTime createdAt, PrimarySymptom primarySymptom, Severity severity) {
		Episode episode = episodeRepository.save(Episode.create(accountId, Symptom.create(90.0, 0.5, primarySymptom, severity)));
		entityManager.flush();
		entityManager.createQuery("UPDATE Episode e SET e.createdAt = :createdAt WHERE e.id = :id")
				.setParameter("createdAt", createdAt)
				.setParameter("id", episode.getId())
				.executeUpdate();
		entityManager.flush();
		entityManager.clear();
		return episode.getId();
	}
}
