package com.afterglow.domain.episode.routine.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.domain.episode.routine.domain.Routine;
import com.afterglow.domain.episode.routine.domain.RoutineRepository;

public interface RoutineJpaRepository extends JpaRepository<Routine, Long>, RoutineRepository {
}
