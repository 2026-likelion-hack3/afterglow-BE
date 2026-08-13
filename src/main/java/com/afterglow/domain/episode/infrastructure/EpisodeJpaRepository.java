package com.afterglow.domain.episode.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.domain.episode.domain.Episode;
import com.afterglow.domain.episode.domain.EpisodeRepository;

public interface EpisodeJpaRepository extends JpaRepository<Episode, Long>, EpisodeRepository {
}
