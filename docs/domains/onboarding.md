# Onboarding

## Responsibility

가벼운 온보딩과 진입 — 기능명세서 1장.

## Current Status

**1.1 기본 정보 입력(연령대/월경 상태) 구현 완료.** 1.2 홈 대시보드, 1.3 지연 회원가입 UI 트리거는 미구현.

## Planned Features

- **1.1 기본 정보 입력**: 연령대, 월경 상태 2문항(피부 타입은 수집하지 않음 — Confirmed Decisions 참고). 모든 문항 건너뛰기 가능, 설치~홈 진입 60초 이내.
- **1.2 홈 대시보드**: 상태 한 줄, "피부가 불편해요" 진입 버튼, 루틴 체크, 오늘 기록을 한 화면에 배치. 여러 도메인 데이터를 조합하는 화면이라, 실제 구현 시점에 onboarding 단독 책임인지 재검토가 필요할 수 있다.
- **1.3 지연 회원가입**: 첫 에피소드(3일차 판정) 완료 후 회원가입을 제안. 실제 회원가입 매커니즘은 `account` 도메인의 이메일 인증 흐름을 그대로 사용한다.

## Domain Model

- `Onboarding`(id, accountId, ageRange nullable, menstrualStatus nullable, onboardingCompletedAt nullable) — `account_id` UNIQUE, 계정당 한 row.
- `AgeRange`: `FORTY_TO_FORTY_FOUR`, `FORTY_FIVE_TO_FORTY_NINE`, `FIFTY_TO_FIFTY_FOUR`, `FIFTY_FIVE_TO_FIFTY_NINE`, `SIXTY_OR_OLDER`
- `MenstrualStatus`: `REGULAR`, `IRREGULAR`, `SKIPPED_TWO_MONTHS_OR_MORE`, `MENOPAUSE_ONE_YEAR_OR_MORE`, `ABSENT_DUE_TO_SURGERY_OR_TREATMENT`, `PREFER_NOT_TO_ANSWER`
- `onboardingCompletedAt`은 최초 제출 시점에만 채워지고 이후 재제출로 값이 바뀌지 않는다("한 번이라도 온보딩을 마쳤는지"를 나타내는 시각).

## API

- `GET /api/onboarding` — row가 없으면 모든 필드가 null인 200(row를 생성하지 않음).
- `PUT /api/onboarding` — idempotent upsert. 둘 다 null(전체 스킵)도 허용하며 완료 시각은 그대로 기록된다. 완료 후 재호출로 값 수정 가능.
- 둘 다 `@AuthenticationPrincipal Long accountId`만 사용, 존재하지 않는(삭제된) 계정이면 404.

## Dependencies

`account` — 온보딩 데이터를 저장하려면 이미 존재하는 익명 `accountId`가 전제된다. `Account` 삭제 시 `AccountDeletedEvent`(`@TransactionalEventListener(BEFORE_COMMIT)`)로 `Onboarding` row도 같은 트랜잭션에서 함께 삭제된다.

## Confirmed Decisions

- **온보딩에서는 피부 타입을 수집하지 않는다.** 연령대, 월경 상태 2문항만 받는다.
- 온보딩 응답은 앱 최초 실행 시 이미 만들어진 익명 `accountId` 기준으로 **서버에 저장**한다 — 기기 로컬 저장 후 회원가입 시점에 별도로 서버에 이관하는 절차를 두지 않는다(`account` 도메인의 익명 계정 우선 설계를 그대로 활용).
- 추후 이메일 인증으로 회원 전환하더라도 같은 `Account`를 유지한다. 새 계정으로 옮기지 않는다.

## Pending Decisions

없음.

## Source of Truth

기능명세서 1장.
