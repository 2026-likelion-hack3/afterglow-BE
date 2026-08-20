# Afterglow Backend

잠과 피부 — 갱년기 여성 스킨케어 루틴 앱 백엔드. 기능/비즈니스 요구사항의 source of truth는 Manyfast(저장소 외부)의 `잠과 피부 — 갱년기 여성 스킨케어 루틴 앱_기능명세서_2026-08-12.md`다.

## 기술 스택

- Java 21
- Spring Boot 3.3.4 (Web, Data JPA, Validation, Actuator)
- Gradle 8.10 (Wrapper 포함, 별도 설치 불필요)
- PostgreSQL (JDBC 드라이버만 포함, 서버 별도 준비 필요 — 메이저 버전은 팀에서 사용 중인 로컬/CI 기준 **18**)
- AWS SDK v2 — SES(이메일 인증코드 발송) 연동 완료. S3(사진 임시 저장)는 아직 미도입
- Docker — 애플리케이션 컨테이너화(multi-stage `Dockerfile`)와 PR CI에서의 이미지 빌드 검증 완료. Registry push(GHCR)와 CD, AWS EC2/RDS 배포는 아직 미도입 — 자세한 내용은 [docs/deployment.md](docs/deployment.md) 참고

## 사전 조건

- JDK 21 (`java -version`으로 확인)
- PostgreSQL (Docker Compose 또는 로컬 설치)
- 별도 Gradle 설치 불필요 (Gradle Wrapper 사용)

## 빠른 시작

```
cp .env.example .env        # Windows는 copy .env.example .env
docker compose up -d        # 로컬 PostgreSQL
./gradlew bootRun           # 프로파일 미지정 시 local 자동 적용 (Windows는 gradlew.bat)
```

PostgreSQL 준비(Docker Compose/직접 설치), 환경변수, 빌드/테스트 명령, IntelliJ 설정, 트러블슈팅 등 상세 절차는 [docs/local-setup.md](docs/local-setup.md)를 참고한다.

## 배포 (prod 프로파일)

환경변수 전체 목록, AWS EC2/RDS 배포 구조와 절차는 [docs/deployment.md](docs/deployment.md) 참고.

## 패키지 구조

`com.afterglow.domain.{업무}` 아래에 기능명세서의 장(章) 구성을 따라 도메인별 패키지(`onboarding`/`episode`/`vanity`/`tracking`/`trust`/`notification`/`account`/`story`)를 두고, 공유 코드는 `com.afterglow.global`에 둔다. 각 패키지의 관계와 설계 배경은 [docs/architecture.md](docs/architecture.md)에, 도메인별 구현 상태와 설계 결정은 [docs/domains/](docs/domains/)에 정리되어 있다.

## 기여 / 작업 흐름

Issue 생성 → 브랜치 → 커밋 → PR 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고한다. PR을 열면 CI(`./gradlew clean build`)가 자동 실행된다.

## 민감정보 관리

`.env`, 실제 비밀번호, API 키 등 민감정보는 저장소에 커밋하지 않는다. contributor 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md), 운영 환경의 실제 secret 관리 방식은 [docs/deployment.md](docs/deployment.md)를 참고한다.

## 더 읽어보기

- [CLAUDE.md](CLAUDE.md) — 문서 라우터, 항상 지킬 원칙
- [CONTRIBUTING.md](CONTRIBUTING.md) — 브랜치/커밋/PR 규칙
- [docs/architecture.md](docs/architecture.md) — 아키텍처, 데이터 저장 경계, 얼굴 사진 처리 원칙
- [docs/conventions.md](docs/conventions.md) — 코드 컨벤션
- [docs/domains/](docs/domains/) — 도메인별 구현 상태와 설계 결정
- [docs/api.md](docs/api.md) — API 계약 공통 규칙 (엔드포인트 목록은 Swagger 참고)
- [docs/erd.md](docs/erd.md) — 엔티티 관계 요약
- [docs/workflow.md](docs/workflow.md) — 브랜치 전략, 기획 미확정 시 대응 원칙
- [docs/deployment.md](docs/deployment.md) — 배포, 환경변수
- [docs/local-setup.md](docs/local-setup.md) — 로컬 개발 환경 설정, 트러블슈팅
