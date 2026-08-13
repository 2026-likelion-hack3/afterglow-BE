# API

## API 계약의 source of truth

이 문서는 엔드포인트 목록을 유지하지 않는다. 실제 요청/응답 스키마는 항상 Swagger(OpenAPI)를 기준으로 확인한다.

- 로컬 실행 중: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI 원본 JSON: `http://localhost:8080/v3/api-docs`

## 공통 규칙

- 에러 응답은 `{code, message}` 형태로 고정한다(`global.exception.ErrorResponse`).
- 인증이 필요한 API는 `Authorization: Bearer <accessToken>` 헤더를 사용한다(JWT).
- 인증된 사용자 식별은 서버에서 `@AuthenticationPrincipal Long accountId`로 꺼낸다. Body/Query/Header로 accountId를 별도로 받지 않는다.
- Swagger에서 인증이 필요한 API는 `bearerAuth` security scheme이 적용되어 자물쇠 아이콘으로 표시된다. Authorize 버튼으로 토큰을 입력하면 이후 요청에 자동으로 헤더가 붙는다. 공개 API(`SecurityConfig`의 permit-all 목록)에는 이 표시가 없다.

## 현재 인증 구현 상태 (코드 기준)

- 현재 코드에는 refresh token 발급/재발급 엔드포인트가 없다. Access token 하나만 발급한다.
- Access token 만료 시간은 `afterglow.jwt.expiration-seconds` 설정값을 따른다(기본값은 `application.yml` 참고, 배포 시 `JWT_EXPIRATION_SECONDS` 환경변수로 재정의 가능).
- 이 설정을 의도적인 "장기 토큰 정책"으로 문서화하지 않는다 — 현재 구현이 이렇다는 사실만 기록한다. 토큰 만료/재발급 정책은 아직 별도로 논의된 적이 없다.
