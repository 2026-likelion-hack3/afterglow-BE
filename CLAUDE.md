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

## 외부 지식 소스

- 기능/비즈니스 요구사항 → Manyfast
- UI/화면 흐름/디자인 → Figma
- 기술 아키텍처/코딩 컨벤션 → repository docs/
- 팀 결정사항/회의 내용 → Notion

여러 source가 충돌하면 임의로 해석하지 않고 충돌 내용을 먼저 사용자에게 보고한다.

## 외부 서비스 쓰기 제한

- 사용자가 명시적으로 요청하지 않는 한 Notion 콘텐츠를 수정하지 않는다.
- 사용자가 명시적으로 요청하지 않는 한 Figma 콘텐츠를 수정하지 않는다.
