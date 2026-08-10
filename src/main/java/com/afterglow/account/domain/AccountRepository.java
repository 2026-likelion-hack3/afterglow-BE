package com.afterglow.account.domain;

import java.util.Optional;

public interface AccountRepository {

	Account save(Account account);

	Optional<Account> findById(Long id);

	Optional<Account> findByEmailAndEmailVerifiedAtIsNotNull(String email);

	boolean existsByEmailAndEmailVerifiedAtIsNotNull(String email);

	void delete(Account account);
}
