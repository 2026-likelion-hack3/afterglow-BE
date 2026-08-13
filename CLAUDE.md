# Afterglow Backend — Claude Code Guide

이 문서는 라우터 역할만 한다. 상세 내용은 각 문서를 참고한다.

- 아키텍처/패키지 구조 → docs/architecture.md
- 코드 컨벤션 → docs/conventions.md
- 도메인별 설계/현재 상태 → docs/domains/*.md
- API 계약 → Swagger(/swagger-ui, /v3/api-docs) — 여기엔 API 목록을 적지 않는다
- 로컬 개발 환경 → docs/local-setup.md
- 배포 → docs/deployment.md
- 브랜치/커밋/PR 규칙 → CONTRIBUTING.md

## 항상 지킬 원칙

- 기능명세서/기획에서 확정되지 않은 비즈니스 규칙(점수, threshold, taxonomy 등)을 임의로 만들지 않는다. 미확정이면 해당 도메인 문서(docs/domains/*.md)의 Pending Decisions로 남긴다.
- 관련 없는 도메인을 리팩터링하지 않는다.
- 인증된 사용자 식별은 항상 `@AuthenticationPrincipal Long accountId` — body/query/header로 따로 받지 않는다.
- 작업 완료 전 항상 clean build.
- `dev`가 integration branch, `main`은 release/production branch.
