package com.afterglow.domain.episode.routine.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoutineRepository {

	Routine save(Routine routine);

	boolean existsByEpisodeId(Long episodeId);

	Optional<Routine> findByEpisodeIdAndAccountId(Long episodeId, Long accountId);

	/**
	 * 계정의 모든 Routine을 시작일 오름차순으로 반환한다 — 제품 중단/재개는 하나의 Episode 안에서 끝나지
	 * 않고(중단은 Episode A의 Routine, 재개는 나중에 생긴 Episode B의 Routine일 수 있다) 계정 전체
	 * Routine 이력을 시간순으로 훑어야 판단할 수 있다({@link RoutineProductLifecycle} 참고).
	 *
	 * <p>{@code start_date}에는 계정 단위 unique 제약이 없고(episode_id만 unique), Service 로직도 "계정에
	 * 이미 진행 중인 Routine이 있는지"는 검사하지 않는다 — 서로 다른 Episode가 같은 날짜에 Routine을 시작할
	 * 수 있다. 그래서 startDate만으로는 동点 시 순서가 DB에 따라 비결정적이라, id(IDENTITY 자동증가 —
	 * 실제 생성 순서와 일치)를 secondary key로 둬 항상 같은 순서를 보장한다.
	 */
	List<Routine> findByAccountIdOrderByStartDateAscIdAsc(Long accountId);

	void deleteByEpisodeIdIn(Collection<Long> episodeIds);
}
