package com.afterglow.domain.episode.application;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.afterglow.domain.account.domain.AccountDeletedEvent;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;
import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;

import lombok.RequiredArgsConstructor;

/**
 * 계정 삭제 시 그 계정 소유의 Episode(및 하위 CheckIn)를 정리한다 — Onboarding과 동일하게 BEFORE_COMMIT을
 * 써서 AccountService.deleteAccount의 트랜잭션에 포함시킨다(cleanup 실패 시 계정 삭제까지 롤백).
 *
 * <p>CheckIn은 `episodeId` 값 참조만 갖고 FK가 없어(#37) DB 레벨 cascade가 없으므로, Episode를 지우기
 * 전에 그 Episode들의 CheckIn을 먼저 지워야 orphan이 남지 않는다 — 순서: 계정 소유 Episode 조회 →
 * 그 Episode들의 CheckIn 삭제 → Episode 삭제. CheckIn에 accountId를 추가하지 않고 episodeId 경유로만
 * 정리한다.
 *
 * <p>Episode의 다른 서브도메인(analysis/card/routine/day3 판정)은 현재 DB에 저장하지 않으므로(순수
 * 함수로만 존재) 정리 대상이 아니다.
 */
@Component
@RequiredArgsConstructor
public class EpisodeAccountDeletedListener {

	private final EpisodeRepository episodeRepository;
	private final CheckInRepository checkInRepository;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onAccountDeleted(AccountDeletedEvent event) {
		List<Long> episodeIds = episodeRepository.findByAccountId(event.accountId()).stream()
				.map(Episode::getId)
				.toList();
		if (!episodeIds.isEmpty()) {
			checkInRepository.deleteByEpisodeIdIn(episodeIds);
		}
		episodeRepository.deleteByAccountId(event.accountId());
	}
}
