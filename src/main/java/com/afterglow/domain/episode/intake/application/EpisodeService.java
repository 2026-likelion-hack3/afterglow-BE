package com.afterglow.domain.episode.intake.application;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.global.exception.NotFoundException;
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

	/** repository 호출이 하나뿐이고(Spring Data가 자체적으로 트랜잭션을 보장) 그 외 원자성이 필요한 동작이 없어 별도 @Transactional을 두지 않는다. */
	public Long createEpisode(Long accountId, Symptom symptom) {
		Episode episode = episodeRepository.save(Episode.create(accountId, symptom));
		return episode.getId();
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
