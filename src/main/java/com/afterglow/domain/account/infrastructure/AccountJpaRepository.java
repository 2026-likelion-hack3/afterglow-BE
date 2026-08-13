package com.afterglow.domain.account.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;

public interface AccountJpaRepository extends JpaRepository<Account, Long>, AccountRepository {
}
