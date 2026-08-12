package com.afterglow.episode.intake.application;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.common.exception.NotFoundException;
import com.afterglow.episode.intake.domain.BodyPart;
import com.afterglow.episode.intake.domain.Episode;
import com.afterglow.episode.intake.domain.EpisodeRepository;
import com.afterglow.episode.intake.domain.Intake;
import com.afterglow.episode.intake.domain.Symptom;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class EpisodeService {

	private final EpisodeRepository episodeRepository;

	public Long createEpisode(Long accountId, Symptom symptom) {
		Episode episode = episodeRepository.save(Episode.create(accountId, symptom));
		return episode.getId();
	}

	public void submitIntake(Long accountId, Long episodeId, Intake intake, Set<BodyPart> bodyParts) {
		Episode episode = episodeRepository.findByIdAndAccountId(episodeId, accountId)
				.orElseThrow(() -> new NotFoundException("에피소드를 찾을 수 없습니다."));
		episode.submitIntake(intake, bodyParts);
	}
}
