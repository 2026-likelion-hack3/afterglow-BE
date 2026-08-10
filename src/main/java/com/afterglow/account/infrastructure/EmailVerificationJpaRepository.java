package com.afterglow.account.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.account.domain.EmailVerification;
import com.afterglow.account.domain.EmailVerificationRepository;

public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerification, Long>, EmailVerificationRepository {
}
