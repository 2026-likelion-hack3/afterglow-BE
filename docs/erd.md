# ERD

## Source of truth

엔티티 정의(필드, 타입, 관계)의 정본은 각 도메인의 `domain/{업무}/domain` 패키지(또는 `domain.vanity`처럼 하위 구조가 아직 없는 도메인은 그 패키지 자체) 코드다. 이 문서는 자동 생성되지 않으며 도메인 간 관계를 사람이 읽기 쉽게 요약하는 용도로만 쓴다. 코드와 이 문서가 어긋나면 코드가 맞다 — 이 문서를 갱신한다.

## 관계 표기 방식

이 프로젝트는 도메인 간에 JPA 연관관계(`@OneToOne`, `@ManyToOne` 등)나 FK를 쓰지 않고, `Product.accountId`처럼 **ID 값 참조**만 쓰는 패턴을 일관되게 사용한다. 도메인 경계를 명확히 하고 aggregate를 독립적으로 다루기 위한 선택이다.

## 현재 구현된 엔티티 (코드 기준)

**account**
- `Account`(id, email nullable, emailVerifiedAt nullable)
- `EmailVerification`(email, code, purpose, expiresAt, consumedAt nullable) — `Account`와 FK 없음, `email`+`purpose`로 조회해서 매칭

**episode (intake만 구현)**
- `Episode`(accountId, status, symptom `@Embedded`, intake `@Embedded`, bodyParts `@ElementCollection`)
- `Symptom`, `Intake`는 `Episode`에 종속된 값 객체(Embeddable) — 별도 테이블/ID 없음

**vanity (엔티티만 구현, Repository/Service/Controller 없음)**
- `Product`(accountId, name, brand, type, keyIngredients, functionTags, openedAt, usageTiming, interactionTags, registrationSource, barcode nullable, photoKey nullable)
- `CombinationRule`(tagA, tagB nullable, minCount, warningMessage) — `Product`와 직접 관계 없음, 태그 조합만으로 판단

**onboarding**
- `Onboarding`(accountId, ageRange nullable, menstrualStatus nullable, onboardingCompletedAt nullable) — `accountId` UNIQUE, `Account`와 FK 없음. `Account` 삭제 시 이벤트로 함께 삭제된다.

## 아직 엔티티가 없는 도메인

`episode.analysis`(설계는 확정됐으나 엔티티 미구현) `/card/routine/checkin`, `trust`, `story`, `tracking`, `notification`은 엔티티가 없다. 이 문서에 구체적 스키마를 임의로 넣지 않는다. 각 도메인의 확정/미확정 설계는 [docs/domains/](domains/)를 참고한다.
