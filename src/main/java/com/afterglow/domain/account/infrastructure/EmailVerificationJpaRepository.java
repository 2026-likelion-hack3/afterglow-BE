package com.afterglow.domain.account.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.domain.account.domain.EmailVerification;
import com.afterglow.domain.account.domain.EmailVerificationRepository;

public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerification, Long>, EmailVerificationRepository {
}
