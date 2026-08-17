package com.afterglow.domain.episode.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.afterglow.domain.account.application.AccountService;
import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.episode.checkin.application.CheckInService;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;
import com.afterglow.domain.episode.checkin.domain.CheckInStatus;
import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.Intake;
import com.afterglow.domain.episode.domain.OnsetPeriod;
import com.afterglow.domain.episode.domain.PrimarySymptom;
import com.afterglow.domain.episode.domain.Severity;
import com.afterglow.domain.episode.domain.Symptom;
import com.afterglow.domain.episode.intake.application.EpisodeService;
import com.afterglow.domain.onboarding.application.OnboardingService;
import com.afterglow.domain.onboarding.domain.AgeRange;
import com.afterglow.domain.onboarding.domain.MenstrualStatus;
import com.afterglow.domain.onboarding.domain.OnboardingRepository;
import com.afterglow.global.exception.NotFoundException;

/**
 * 계정 삭제 시 Episode/CheckIn cleanup(Issue #32)이 실제로 커밋되는지 검증한다. BEFORE_COMMIT 리스너는
 * 실제 물리 커밋에서만 실행되므로, EpisodeServiceTest/CheckInServiceTest와 동일한 이유로 테스트 레벨
 * @Transactional을 붙이지 않는다 — 그래야 accountService.deleteAccount 호출이 진짜 커밋되고, 이후
 * 재조회가 실제 DB 상태를 그대로 반영한다.
 */
@SpringBootTest
class EpisodeAccountDeletedListenerTest {

	@Autowired
	private AccountService accountService;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private EpisodeService episodeService;

	@Autowired
	private EpisodeRepository episodeRepository;

	@Autowired
	private CheckInService checkInService;

	@Autowired
	private CheckInRepository checkInRepository;

	@Autowired
	private OnboardingService onboardingService;

	@Autowired
	private OnboardingRepository onboardingRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	/**
	 * createEpisode(계정 존재 확인 + Episode 저장)와 deleteAccount(계정 삭제 + Episode cleanup) 사이의
	 * race condition이 실제로 직렬화되는지 검증한다. createEpisode 쪽 트랜잭션을 테스트에서 직접 열고
	 * PESSIMISTIC_READ lock을 잡은 채로 CountDownLatch로 붙들어, 그동안 별도 스레드에서 deleteAccount를
	 * 실행한다 — production 코드에 인위적인 지연/실패 hook을 넣지 않고 순수하게 실제 DB row lock으로
	 * 대기시킨다.
	 *
	 * <p>이 테스트는 "그 순간 실제로 블로킹됐는지"를 타이밍으로 단언하지 않는다(그건 스레드 스케줄링에
	 * 따라 흔들릴 수 있어 flaky해진다) — 대신 PESSIMISTIC_READ와 DELETE의 실제 lock 충돌 덕분에, 어떤
	 * 순서로 실행되든 "두 트랜잭션이 겹쳐 있는 동안에는 delete가 끝날 수 없다"는 DB 레벨 보장만으로
	 * 최종 상태(orphan Episode 없음)가 항상 성립한다는 점을 확인한다.
	 */
	@Test
	void 계정_삭제와_Episode_생성이_동시에_발생해도_orphan_Episode가_남지_않는다() throws InterruptedException {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		CountDownLatch createLockAcquired = new CountDownLatch(1);
		CountDownLatch proceedWithSave = new CountDownLatch(1);
		AtomicReference<Throwable> createFailure = new AtomicReference<>();
		AtomicReference<Throwable> deleteFailure = new AtomicReference<>();

		Thread createThread = new Thread(() -> {
			try {
				transactionTemplate.executeWithoutResult(status -> {
					accountRepository.findByIdForUpdate(accountId);
					createLockAcquired.countDown();
					awaitQuietly(proceedWithSave);
					episodeRepository.save(Episode.create(accountId, symptom()));
				});
			} catch (Throwable e) {
				createFailure.set(e);
			}
		});

		createThread.start();
		assertThat(createLockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

		Thread deleteThread = new Thread(() -> {
			try {
				accountService.deleteAccount(accountId);
			} catch (Throwable e) {
				deleteFailure.set(e);
			}
		});
		deleteThread.start();

		// delete 스레드가 DELETE 문을 실제로 시도(하고 lock 대기에 들어갈)할 시간을 잠깐 준다 — 이 sleep은
		// "블로킹을 관찰"하기 위한 게 아니라 두 트랜잭션이 겹치는 케이스를 더 확실히 유도하기 위한 것일 뿐,
		// 정답 여부는 sleep 시간과 무관하게 lock으로 보장된다.
		Thread.sleep(200);
		proceedWithSave.countDown();

		createThread.join(5000);
		deleteThread.join(5000);

		assertThat(createFailure.get()).isNull();
		assertThat(deleteFailure.get()).isNull();
		assertThat(episodeRepository.findByAccountId(accountId)).isEmpty();
		assertThat(accountRepository.findById(accountId)).isEmpty();
	}

	private void awaitQuietly(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Test
	void 계정을_삭제하면_소유한_Episode가_삭제된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();

		accountService.deleteAccount(accountId);

		assertThat(episodeRepository.findByIdAndAccountId(episodeId, accountId)).isEmpty();
		assertThat(episodeRepository.findByAccountId(accountId)).isEmpty();
	}

	@Test
	void 계정을_삭제하면_그_Episode의_CheckIn도_삭제된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		checkInService.record(accountId, episodeId, LocalDate.of(2026, 8, 17), CheckInStatus.SAME);

		accountService.deleteAccount(accountId);

		assertThat(checkInRepository.findByEpisodeIdOrderByCheckInDateAsc(episodeId)).isEmpty();
	}

	@Test
	void 다른_계정의_Episode와_CheckIn은_영향받지_않는다() {
		Long deletedAccountId = accountRepository.save(Account.createAnonymous()).getId();
		Long deletedEpisodeId = episodeRepository.save(Episode.create(deletedAccountId, symptom())).getId();
		checkInService.record(deletedAccountId, deletedEpisodeId, LocalDate.of(2026, 8, 17), CheckInStatus.SAME);

		Long keptAccountId = accountRepository.save(Account.createAnonymous()).getId();
		Long keptEpisodeId = episodeRepository.save(Episode.create(keptAccountId, symptom())).getId();
		checkInService.record(keptAccountId, keptEpisodeId, LocalDate.of(2026, 8, 17), CheckInStatus.IMPROVED);

		accountService.deleteAccount(deletedAccountId);

		assertThat(episodeRepository.findByIdAndAccountId(keptEpisodeId, keptAccountId)).isPresent();
		assertThat(checkInRepository.findByEpisodeIdOrderByCheckInDateAsc(keptEpisodeId)).hasSize(1);
	}

	@Test
	void Onboarding_Episode_CheckIn을_모두_가진_계정을_삭제하면_전부_정리된다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		onboardingService.save(accountId, AgeRange.FIFTY_TO_FIFTY_FOUR, MenstrualStatus.IRREGULAR);
		Long episodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();
		checkInService.record(accountId, episodeId, LocalDate.of(2026, 8, 17), CheckInStatus.WORSE);

		accountService.deleteAccount(accountId);

		assertThat(onboardingRepository.findByAccountId(accountId)).isEmpty();
		assertThat(episodeRepository.findByAccountId(accountId)).isEmpty();
		assertThat(checkInRepository.findByEpisodeIdOrderByCheckInDateAsc(episodeId)).isEmpty();
	}

	@Test
	void 삭제_후_기존_episodeId로는_문진을_제출할_수_없다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		Long episodeId = episodeRepository.save(Episode.create(accountId, symptom())).getId();

		accountService.deleteAccount(accountId);

		assertThatThrownBy(() -> episodeService.submitIntake(accountId, episodeId, intake(), Set.of(BodyPart.CHEEK)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void 삭제된_계정으로는_새_Episode를_생성할_수_없다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();
		accountService.deleteAccount(accountId);

		assertThatThrownBy(() -> episodeService.createEpisode(accountId, symptom()))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void 정상_계정은_Episode를_생성할_수_있다() {
		Long accountId = accountRepository.save(Account.createAnonymous()).getId();

		Long episodeId = episodeService.createEpisode(accountId, symptom());

		assertThat(episodeRepository.findByIdAndAccountId(episodeId, accountId)).isPresent();
	}

	private Symptom symptom() {
		return Symptom.create(90.0, 0.5, PrimarySymptom.REDNESS, Severity.MODERATE);
	}

	private Intake intake() {
		return Intake.create(OnsetPeriod.TODAY, "새 앰플", null);
	}
}
