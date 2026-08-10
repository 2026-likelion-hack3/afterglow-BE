package com.afterglow.account.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.account.domain.Account;
import com.afterglow.account.domain.AccountRepository;

public interface AccountJpaRepository extends JpaRepository<Account, Long>, AccountRepository {
}
