# Architecture

## 현재 형태: 모듈러 모놀리스

Afterglow Backend는 현재 **단일 Spring Boot 애플리케이션**으로 배포되는 모듈러 모놀리스다. 여러 서비스로 분리되어 있지 않으며, 도메인 간 경계는 패키지 단위로만 구분된다. 별도 서비스 분리는 이번 범위에 포함되지 않으며, 필요성이 확인되기 전까지 계획하지 않는다.

## 패키지 최상위 구조

```
com.afterglow
├── global          # 도메인 전반에서 공유하는 베이스 엔티티, 예외 처리, 보안 설정
└── domain
    └── {업무 도메인}  # 기능명세서 장(章) 단위
```

`com.afterglow.domain.{업무}.domain`처럼 `domain`이라는 이름이 두 번 나오는 경우가 있다. 첫 번째 `domain`은 **업무 도메인을 묶는 최상위 그룹**(패키지 구조상의 분류)이고, 두 번째 `domain`은 그 업무 도메인 안의 **Domain Layer**(Entity, 도메인 규칙, Repository 인터페이스)를 뜻한다. 서로 다른 의미의 동명이지만, 레이어 용어 자체(`api`/`application`/`domain`/`infrastructure`)는 이미 굳어진 컨벤션이라 이번 구조 개편에서 바꾸지 않기로 했다.

## 도메인 중심 패키지 구조를 사용하는 이유

`com.afterglow.domain` 하위 패키지는 계층(controller/service/repository)이 아니라 **기능명세서의 장(章) 구성**을 기준으로 나뉜다. 화면·기능 단위로 코드를 모아두면, 기능명세서를 기준으로 코드를 찾고 리뷰할 수 있고, 각 도메인이 독립적으로 성장하거나 필요 시 분리될 여지를 남긴다.

## 최상위 업무 도메인 책임

| 패키지 | 책임 (기능명세서 장) |
|---|---|
| `domain.onboarding` | 가벼운 온보딩과 진입 (1장) |
| `domain.episode` | 증상 접수부터 3일차 판정까지 하나의 에피소드로 묶이는 흐름 (2~4장) |
| `domain.vanity` | 화장대 — 보유 제품 등록과 조합 주의 경고 (5장) |
| `domain.tracking` | 상시 루프 — 수면·컨디션 1탭 기록과 주간 리포트 (6장) |
| `domain.trust` | 상업적 중립성 고지, 금지 용어 목록, 사진 데이터 처리 안내 (7장) |
| `domain.notification` | 맞춤 알림 설정과 발송 (8장) |
| `domain.account` | 계정 생성·로그인·데이터 삭제 (9장) |
| `domain.story` | 이야기 — 증상 태그로 연결되는 커뮤니티 (10장) |
| `global` | 도메인 전반에서 공유하는 베이스 엔티티, 예외 처리, 보안 설정 |

각 패키지의 `package-info.java`가 이 표의 정본이다. 이 문서와 `package-info.java` 설명이 어긋나면 `package-info.java`를 기준으로 이 문서를 갱신한다. **도메인별 구현 상태, 설계 결정, 확정/미확정 사항은 [docs/domains/](domains/)를 참고한다 — 이 문서에서 중복 기술하지 않는다.**

## episode 하위 패키지 관계

`episode`는 "증상 접수 → 분석 → 결과 카드 → 3일 루틴 → 체크인"이라는 하나의 순차적 흐름(에피소드)을 표현하며, 하위 패키지는 그 흐름의 단계에 대응한다.

```
intake (2.1~2.3 증상 격자 선택, 문진, 사진)
  → analysis (2.4 통합 분석 — 원인 후보·확신 수준·근거 기록 일수)
    → card (3.1, 4.3 결과 카드 3장: 중단/사용/병원, 빈 범주 안내)
      → routine (3.2 3일 루틴 생성)
        → checkin (4.1~4.2 일일 1탭 체크, 3일차 판정 분기)
```

하위 패키지 간 의존은 이 흐름 순서(위→아래)를 따른다. 역방향 의존(예: `intake`가 `checkin`을 참조)은 지양한다.

Episode aggregate 자체(`Episode`, `EpisodeStatus`, `Symptom`, `Intake` 등)는 여러 서브도메인이 공유하는 루트라서 `domain.episode.domain`(서브도메인 바로 아래가 아니라 `episode` 도메인의 공유 위치)에 둔다. 각 서브도메인 고유 로직(`intake.api`, `intake.application` 등)만 그 서브도메인 패키지 안에 둔다.

## global 패키지에 둘 수 있는 것 / 둘 수 없는 것

**둘 수 있는 것**
- 모든 도메인 엔티티가 상속하는 베이스 타입(`BaseEntity`).
- 공통 예외 타입과 전역 예외 처리(`AfterglowException`, `ErrorCode`, `ErrorResponse`, `GlobalExceptionHandler`).
- 특정 도메인에 속하지 않는 애플리케이션 전역 설정(`JpaAuditingConfig`, 인증/보안 설정 등).

**둘 수 없는 것**
- 특정 도메인에서만 쓰이는 로직이나 DTO. 두 도메인에서 우연히 비슷한 코드가 필요하다고 바로 `global`로 옮기지 않는다 — 세 번째 사용처가 생기기 전까지는 각 도메인에 둔다.
- 도메인 규칙(예: 화장대 조합 경고 규칙, 3일차 판정 로직)은 `global`이 아니라 해당 도메인 패키지에 둔다.
- 아직 실제로 쓰이지 않는 인프라 설정을 미리 만들어두지 않는다.

## 도메인 간 의존 방향

- 원칙적으로 도메인 패키지는 서로 직접 의존하지 않는다. 도메인 간 연동이 필요하면(예: `episode`의 판정 결과가 `tracking`의 주간 리포트에 반영) 이벤트나 명시적인 애플리케이션 서비스 호출 등 느슨한 결합을 우선 검토한다. 구체적인 연동 방식은 실제 구현 시점에 별도로 정한다.
- 모든 도메인은 `global`에 의존할 수 있지만, `global`은 어떤 도메인에도 의존하지 않는다.

## 계층 구조: API → Application → Domain, Infrastructure는 Domain을 구현

도메인이 실제로 구현될 때는 [docs/conventions.md](conventions.md#패키지-규칙)에 정의한 `api / application / domain / infrastructure` 구조를 그 도메인 범위에서 사용한다.

- `api`: Controller, Request/Response DTO. `application`만 호출한다.
- `application`: 유스케이스 조정(Service). `domain`을 사용해 흐름을 조립하고, 트랜잭션 경계를 가진다.
- `domain`: Entity, 도메인 정책/규칙, Repository **인터페이스**. 다른 계층에 의존하지 않는다.
- `infrastructure`: `domain`이 정의한 Repository 인터페이스의 구현체, 외부 시스템 연동(S3, SES 등).

의존 방향은 `api → application → domain ← infrastructure`이며, `domain`이 인터페이스를 정의하고 `infrastructure`가 이를 구현하는 의존성 역전 구조를 따른다.

현재 `domain.vanity`에는 Entity/enum만 있고 Repository/Service/Controller가 없어 이 구조가 아직 적용되지 않았다. 실제 구현 시점에 도입한다.

## 데이터 저장 경계 (추후 확정 필요)

서버에 영구 저장되는 데이터와 모바일 기기(앱)에만 저장되는 데이터의 경계는 아직 확정되지 않았다. 도메인 구현 시점마다 어떤 데이터를 서버 DB에 둘지, 어떤 데이터를 클라이언트에만 둘지 판단이 필요하며, 이 문서는 결정이 이뤄지는 대로 갱신한다.

## 얼굴 사진 처리 원칙

**얼굴 사진 원본은 서버에 영구 저장하지 않는다.** `domain.trust` 패키지(7장)가 사진 데이터 처리 정책을 다루며, `domain.vanity.Product.photoKey`처럼 제품 사진(S3 임시 저장) 용도와는 구분된다. 얼굴 사진과 관련된 기능을 구현할 때는 이 원칙을 벗어나지 않는지 PR 단계에서 확인한다([PR 템플릿](../.github/PULL_REQUEST_TEMPLATE.md) 체크리스트 참고).
