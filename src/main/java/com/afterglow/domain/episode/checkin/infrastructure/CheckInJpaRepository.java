package com.afterglow.domain.episode.checkin.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.domain.episode.checkin.domain.CheckIn;
import com.afterglow.domain.episode.checkin.domain.CheckInRepository;

public interface CheckInJpaRepository extends JpaRepository<CheckIn, Long>, CheckInRepository {
}
