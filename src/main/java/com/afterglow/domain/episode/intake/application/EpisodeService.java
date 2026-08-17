package com.afterglow.domain.episode.intake.application;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.global.exception.NotFoundException;
import com.afterglow.domain.account.domain.AccountRepository;
import com.afterglow.domain.episode.domain.BodyPart;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.domain.episode.domain.Intake;
import com.afterglow.domain.episode.domain.Symptom;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EpisodeService {

	private final EpisodeRepository episodeRepository;
	private final AccountRepository accountRepository;

	/**
	 * episodeId가 아직 없어(생성 그 자체) findByIdAndAccountId 같은 ownership lookup으로 보호할 수 없다 —
	 * 삭제된 계정의 유효한 JWT로 새 Episode가 생기지 않도록 계정 존재를 직접 확인한다.
	 *
	 * <p>단순 존재 확인(findById) + 저장만으로는 race condition이 남는다: 확인이 성공한 직후, 커밋 전에
	 * 다른 트랜잭션이 그 계정을 삭제(+cleanup)하면, 그 뒤 이 메서드의 저장이 커밋되면서 이미 삭제된
	 * accountId를 가진 Episode가 새로 생겨버린다(cleanup은 삭제 시점에 존재하던 Episode만 지우므로 이후
	 * 생긴 Episode는 정리되지 않는다). 그래서 {@code findByIdForUpdate}(PESSIMISTIC_READ)로 계정 row에
	 * 공유 lock을 잡은 채로 저장까지 하나의 트랜잭션에서 처리한다 — `AccountService.deleteAccount`의
	 * DELETE는 배타적 lock이 필요해 이 lock을 잡은 트랜잭션이 끝날 때까지 대기하므로 두 흐름이 직렬화된다:
	 * 이 메서드가 먼저 lock을 잡으면 Episode 저장이 커밋된 뒤에야 삭제(+cleanup)가 진행되고, 삭제가 먼저
	 * 진행되면 이 메서드는 계정을 찾지 못해 실패한다. 같은 계정으로 여러 Episode를 동시에 만드는 요청끼리는
	 * PESSIMISTIC_READ(공유 lock)라 서로 막지 않는다.
	 */
	@Transactional
	public Long createEpisode(Long accountId, Symptom symptom) {
		requireAccountExists(accountId);
		Episode episode = episodeRepository.save(Episode.create(accountId, symptom));
		return episode.getId();
	}

	private void requireAccountExists(Long accountId) {
		accountRepository.findByIdForUpdate(accountId)
				.orElseThrow(() -> new NotFoundException("계정을 찾을 수 없습니다."));
	}

	/**
	 * 조회한 managed Episode를 명시적 save() 없이 dirty checking으로 변경한다(embedded intake 교체,
	 * bodyParts 컬렉션 교체, status 전이). 조회와 변경이 같은 persistence context 안에서 이어져야
	 * flush 시점에 실제로 커밋되므로 @Transactional이 필요하다.
	 */
	@Transactional
	public void submitIntake(Long accountId, Long episodeId, Intake intake, Set<BodyPart> bodyParts) {
		Episode episode = episodeRepository.findByIdAndAccountId(episodeId, accountId)
				.orElseThrow(() -> new NotFoundException("에피소드를 찾을 수 없습니다."));
		episode.submitIntake(intake, bodyParts);
	}
}
