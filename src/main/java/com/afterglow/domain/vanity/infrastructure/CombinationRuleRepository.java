package com.afterglow.domain.vanity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.domain.vanity.CombinationRule;

public interface CombinationRuleRepository extends JpaRepository<CombinationRule, Long> {
}
