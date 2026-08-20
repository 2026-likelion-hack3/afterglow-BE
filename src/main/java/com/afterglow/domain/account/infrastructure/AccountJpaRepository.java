package com.afterglow.domain.account.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.afterglow.domain.account.domain.Account;
import com.afterglow.domain.account.domain.AccountRepository;

import jakarta.persistence.LockModeType;

public interface AccountJpaRepository extends JpaRepository<Account, Long>, AccountRepository {

	@Lock(LockModeType.PESSIMISTIC_READ)
	@Query("SELECT a FROM Account a WHERE a.id = :id")
	Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
