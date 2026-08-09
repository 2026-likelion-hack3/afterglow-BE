# 개발 컨벤션

브랜치/커밋 규칙은 [CONTRIBUTING.md](../CONTRIBUTING.md)를 참고한다. 이 문서는 Java/Spring 코드 작성 규칙과 패키지 구조 규칙을 다룬다.

## Java / Spring 규칙

- **생성자 주입**만 사용한다. `@Autowired` Field Injection은 금지한다.
- Entity를 API Response로 직접 노출하지 않는다. Request/Response DTO를 분리한다.
- Controller는 요청·응답 변환만 담당한다. 비즈니스 로직을 Controller에 두지 않는다.
- Service(Application) 계층은 유스케이스 조정을 담당한다. 도메인 규칙 자체는 도메인 객체 또는 정책 객체에 둔다.
- Repository를 Controller에서 직접 호출하지 않는다. 항상 Service를 거친다.
- public setter를 무분별하게 만들지 않는다. 상태 변경은 의미 있는 이름의 메서드로 노출한다(예: `Product.updateDetails(...)`처럼 현재 `vanity` 도메인에 적용된 패턴 참고).
- 트랜잭션 경계를 명확히 표시한다(`@Transactional`을 Service 계층에 명시).
- 조회 전용 메서드는 `@Transactional(readOnly = true)`를 사용한다.
- enum은 DB에 문자열로 저장한다(`@Enumerated(EnumType.STRING)`). ordinal 저장 금지.
- 시간은 명확한 기준(UTC/KST 등)을 정해 일관되게 다룬다. 현재 `BaseEntity`는 `LocalDateTime` + JPA Auditing을 사용한다.
- 예외를 무분별하게 `RuntimeException`으로 던지지 않는다. 비즈니스 예외는 `AfterglowException`(또는 그 하위 타입)을 사용하고 `ErrorCode`를 명시한다. 공통 예외 구조는 [ARCHITECTURE.md](ARCHITECTURE.md#common-패키지에-둘-수-있는-것--둘-수-없는-것) 참고.
- 한 PR에 관련 없는 변경을 섞지 않는다.

## 패키지 규칙

최상위 도메인 패키지(`onboarding`, `episode`, `vanity`, `tracking`, `trust`, `notification`, `account`, `story`, `common`)는 기능명세서 장 구성을 그대로 따르며 유지한다.

각 도메인이 실제로 구현될 때, 필요한 범위에서 아래 하위 구조를 사용한다.

```
com.afterglow.{domain}
├── api            # Controller, Request/Response DTO
├── application     # Service, 유스케이스 조정
├── domain          # Entity, 도메인 정책, Repository 인터페이스
└── infrastructure  # Repository 구현체, 외부 연동
```

- 현재 비어 있는 도메인 패키지에 하위 구조나 빈 클래스를 미리 일괄 생성하지 않는다. 실제 기능을 구현하는 시점에 필요한 만큼만 만든다.
- `vanity` 도메인은 현재 하위 구조 없이 Entity/enum만 존재한다. Repository/Service/Controller가 추가되는 시점에 위 구조 도입 여부를 판단한다.

## 커밋되지 않아야 하는 것

- 사용하지 않는 추상화, 빈 Repository/Service/Controller.
- 미래에 쓸 것으로 예상되는 설정 클래스(Security, AWS SDK 설정 등 현재 사용처가 없는 것).
