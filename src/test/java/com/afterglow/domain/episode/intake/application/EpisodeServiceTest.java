package com.afterglow.domain.episode.intake.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.EpisodeStatus;
import com.afterglow.domain.episode.domain.Intake;
import com.afterglow.domain.episode.domain.OnsetPeriod;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.domain.Symptom;

/**
 * submitIntake는 명시적 save() 없이 dirty checking으로 Episode를 변경한다. 이 테스트는 그 변경이
 * 실제로 DB에 반영되는지를, EpisodeService 호출을 감싸는 테스트 레벨 트랜잭션 없이(AccountServiceRollbackTest와
 * 동일한 이유로 의도적으로 @Transactional을 붙이지 않는다) 검증한다 — 저장/submitIntake/재조회가
 * 각각 독립된 트랜잭션(=독립된 persistence context)에서 실행되므로, 재조회 결과가 실제 DB 상태를
 * 그대로 반영한다. 메모리상 managed 엔티티 상태가 아니라 DB 재조회 결과만으로 판단한다.
 */
@SpringBootTest
class EpisodeServiceTest {

	@Autowired
	private EpisodeService episodeService;

	@Autowired
	private EpisodeRepository episodeRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void submitIntake으로_반영한_변경사항이_새_트랜잭션의_재조회에서도_유지된다() {
		Long accountId = 5001L;
		Episode saved = episodeRepository.save(Episode.create(accountId, symptom()));
		Long episodeId = saved.getId();

		episodeService.submitIntake(accountId, episodeId, intake(), Set.of(BodyPart.CHEEK, BodyPart.CHIN));

		// status/intake는 @Embedded(즉시 로딩)라 detached 상태에서도 안전하게 읽을 수 있다.
		Episode reloaded = episodeRepository.findByIdAndAccountId(episodeId, accountId).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(EpisodeStatus.INTAKE_COMPLETED);
		assertThat(reloaded.getIntake().getOnsetPeriod()).isEqualTo(OnsetPeriod.TODAY);
		assertThat(reloaded.getIntake().getRecentNewProductName()).isEqualTo("새 앰플");

		// bodyParts는 LAZY @ElementCollection이라 detached 엔티티에서 접근하면 LazyInitializationException이
		// 나므로, Hibernate 세션과 무관하게 raw SQL로 DB에 실제로 저장됐는지 직접 확인한다.
		List<String> bodyParts = jdbcTemplate.queryForList(
				"SELECT body_part FROM episode_body_part WHERE episode_id = ?", String.class, episodeId);
		assertThat(bodyParts).containsExactlyInAnyOrder("CHEEK", "CHIN");
	}

	private Symptom symptom() {
		return Symptom.create(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE);
	}

	private Intake intake() {
		return Intake.create(OnsetPeriod.TODAY, "새 앰플", null);
	}
}
