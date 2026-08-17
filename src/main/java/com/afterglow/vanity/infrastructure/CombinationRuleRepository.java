package com.afterglow.vanity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.vanity.CombinationRule;

public interface CombinationRuleRepository extends JpaRepository<CombinationRule, Long> {
}
