package com.afterglow.episode.intake.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.episode.intake.domain.Episode;
import com.afterglow.episode.intake.domain.EpisodeRepository;

public interface EpisodeJpaRepository extends JpaRepository<Episode, Long>, EpisodeRepository {
}
