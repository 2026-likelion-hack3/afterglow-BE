# Account

## Responsibility

계정 생성, 로그인, 데이터 삭제 — 기능명세서 9장.

## Current Status

**구현 완료.** 익명 계정 생성, 이메일 인증코드 기반 회원가입/로그인, 계정 삭제, SES 이메일 발송까지 merge되어 있다.

## Planned Features

없음 — 기능명세서 9장(9.1 계정 생성과 로그인, 9.2 계정·기록 삭제) 범위가 구현 완료 상태다.

## Domain Model

(`domain/account/domain` 기준)

- `Account`: `id`, `email`(nullable), `emailVerifiedAt`(nullable). 비밀번호 필드 없음.
- `EmailVerification`: `email`, `code`, `purpose`(`SIGNUP`/`LOGIN`), `expiresAt`, `consumedAt`(nullable).
- `VerificationPurpose`: `SIGNUP`, `LOGIN`.

## Dependencies

다른 모든 도메인이 인증이 필요한 API에서 `@AuthenticationPrincipal Long accountId`로 이 도메인이 발급한 토큰을 사용한다. `account`는 다른 업무 도메인에 의존하지 않고, 반대로 다른 도메인들이 account의 인증 메커니즘에 의존하는 방향이다.

## Confirmed Decisions

- **익명 계정 우선**: 앱 최초 실행 시 이메일 없이 익명 `Account`를 생성하고 토큰을 발급한다. "회원가입"은 이 계정에 이메일을 붙이는 행위이며, 새 계정을 만들지 않는다.
- 비밀번호 없이 이메일 인증코드(6자리, 매직링크 아님) 기반 회원가입/로그인.
- JWT는 stateless, refresh token 없이 access token 하나만 사용.
- 이메일 발송은 프로파일별로 구현체가 갈린다: local/test는 `LoggingEmailSender`(로그만 남김), prod는 `SesEmailSender`(AWS SES, EC2 IAM role 자격증명 체인 사용, 키 하드코딩 없음).
- SES 발송 실패는 `EMAIL_SEND_FAILED`(503)로 변환하고, AWS 원문 메시지는 응답에 노출하지 않는다. 원인 로그는 이메일을 마스킹해서 남긴다.
- 계정 삭제는 하드 삭제. 다른 도메인이 실제로 accountId를 참조하는 데이터를 쌓기 시작하기 전까지는 cascade 정리 로직을 만들지 않는다.

## Pending Decisions

없음.

## Source of Truth

- API 계약: Swagger
- 구현 상태/구조: `src/main/java/com/afterglow/domain/account`
