package com.afterglow.domain.account.domain;

import java.util.Optional;

public interface AccountRepository {

	Account save(Account account);

	Optional<Account> findById(Long id);

	/**
	 * id로 조회하면서 PESSIMISTIC_READ(공유) row lock을 잡는다 — Episode 생성처럼 "계정이 존재하는 동안
	 * 다른 트랜잭션이 그 계정을 지우지 못하게" 만들어야 하는 use case 전용이다. 여러 트랜잭션이 동시에 이
	 * lock을 잡아도 서로는 막지 않지만(공유 lock), 계정 삭제(DELETE, 배타적 lock 필요)는 이 lock을 잡은
	 * 트랜잭션이 끝날 때까지 대기한다 — 일반 조회에는 이 메서드를 쓰지 않는다.
	 */
	Optional<Account> findByIdForUpdate(Long id);

	Optional<Account> findByEmailAndEmailVerifiedAtIsNotNull(String email);

	boolean existsByEmailAndEmailVerifiedAtIsNotNull(String email);

	void delete(Account account);

	/**
	 * 보류 중인 변경을 즉시 DB로 내보낸다. `deleteAccount`가 이걸 명시적으로 호출하는 이유: Spring의
	 * JpaTransactionManager는 커밋 직전에 자동으로 flush하지 않는다(flushEagerly가 기본 false) —
	 * {@code delete()}로 표시한 삭제는 실제로는 물리 커밋 시점(BEFORE_COMMIT 리스너 실행 "이후")에야
	 * DB로 나간다. 그래서 이 메서드로 미리 flush해 실제 DELETE를 리스너 실행 전에 동기적으로 내보내야만,
	 * 동시에 그 계정으로 Episode를 생성하려는 트랜잭션의 row lock과 여기서 진짜로 충돌(대기)한다 — 그래야
	 * cleanup 리스너가 항상 "이 시점까지 커밋된 최신 상태"를 보고 정리할 수 있다(Issue #32).
	 */
	void flush();
}
