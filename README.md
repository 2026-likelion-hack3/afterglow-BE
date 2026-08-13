# Afterglow Backend

잠과 피부 — 갱년기 여성 스킨케어 루틴 앱 백엔드. 기능명세서: `잠과 피부 — 갱년기 여성 스킨케어 루틴 앱_기능명세서_2026-08-12.md`

## 기술 스택

- Java 21
- Spring Boot 3.3.4 (Web, Data JPA, Validation, Actuator)
- Gradle 8.10 (Wrapper 포함, 별도 설치 불필요)
- PostgreSQL (JDBC 드라이버만 포함, 서버 별도 준비 필요 — 메이저 버전은 팀에서 사용 중인 로컬/CI 기준 **18**)
- AWS SDK v2 — SES(이메일 인증코드 발송) 연동 완료. S3(사진 임시 저장)는 아직 미도입
- 향후 도입 예정: Docker(로컬 PostgreSQL 외 앱 컨테이너화), AWS EC2/RDS 배포

## 요구 Java 버전

Java 21. `java -version`으로 확인한다.

## 로컬 실행 사전 조건

- JDK 21
- PostgreSQL (Docker Compose 또는 로컬 설치) — 아래 [PostgreSQL 준비](#postgresql-실행-및-연결) 참고
- 별도 Gradle 설치 불필요 (Gradle Wrapper 사용)

## PostgreSQL 실행 및 연결

### Docker Compose 사용 (권장)

저장소 루트의 `docker-compose.yml`은 PostgreSQL 컨테이너만 정의한다(애플리케이션 컨테이너/Dockerfile 없음).

```
cp .env.example .env        # Windows는 copy .env.example .env
docker compose up -d
docker compose ps
docker compose logs -f postgres
```

`docker compose ps`에서 상태가 `healthy`가 되면(내부적으로 `pg_isready`로 확인) 준비된 것이다.

종료:
```
docker compose down
```
> 데이터까지 삭제하려면 `docker compose down -v`를 사용한다. **named volume이 함께 삭제되어 로컬 DB 데이터가 모두 사라진다.**

### 직접 설치한 PostgreSQL 사용

```sql
CREATE USER afterglow WITH PASSWORD 'afterglow';
CREATE DATABASE afterglow OWNER afterglow;
```

자세한 절차와 트러블슈팅은 [docs/local-setup.md](docs/local-setup.md)를 참고한다.

## 환경변수 목록

`.env.example`을 복사해 `.env`로 사용한다(`.env`는 git에 커밋하지 않는다). 전체 환경변수 목록과 prod 배포 관련 내용은 [docs/deployment.md](docs/deployment.md)를 참고한다.

## local 프로파일 실행 방법

`application.yml`은 `spring.profiles.default: local`을 지정하므로 **프로파일을 아무것도 지정하지 않으면 자동으로 local이 적용**된다. 필요하면 명시적으로 지정할 수도 있다.

**Windows**
```
:: 1) 기본 실행 (프로파일 미지정 → local 자동 적용)
gradlew.bat bootRun

:: 2) local 프로파일 명시
gradlew.bat bootRun --args="--spring.profiles.active=local"
```

**macOS/Linux**
```
# 1) 기본 실행 (프로파일 미지정 → local 자동 적용)
./gradlew bootRun

# 2) local 프로파일 명시
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Gradle 빌드 명령

**Windows**
```
gradlew.bat clean build
```

**macOS/Linux**
```
./gradlew clean build
```

## 테스트 명령

**Windows**
```
gradlew.bat test
```

**macOS/Linux**
```
./gradlew test
```

## 애플리케이션 실행 명령

[local 프로파일 실행 방법](#local-프로파일-실행-방법) 참고.

## 배포 (prod 프로파일)

자세한 내용은 [docs/deployment.md](docs/deployment.md) 참고.

## 패키지 구조

`com.afterglow.domain.{업무}` 아래에 기능명세서의 장(章) 구성을 따라 도메인별 패키지를 둔다. 공유 코드는 `com.afterglow.global`에 둔다. 각 패키지의 관계와 설계 배경은 [docs/architecture.md](docs/architecture.md)에, 도메인별 구현 상태와 설계 결정은 [docs/domains/](docs/domains/)에 정리되어 있다.

- `domain.onboarding` — 온보딩(1장)
- `domain.episode` — 증상 접수부터 3일차 판정까지(2~4장): `intake`, `analysis`, `card`, `routine`, `checkin`
- `domain.vanity` — 화장대(5장) — 현재 `Product`, `CombinationRule` 엔티티와 관련 enum만 존재
- `domain.tracking` — 일상 기록·주간 리포트(6장)
- `domain.trust` — 상업적 중립성·금지 용어·사진 처리 정책(7장)
- `domain.notification` — 알림(8장)
- `domain.account` — 계정·데이터 관리(9장)
- `domain.story` — 커뮤니티(10장)
- `global` — 베이스 엔티티, 공통 예외 처리, 보안 설정

## GitHub Issue 및 PR 작업 흐름

1. 작업 전 Issue를 생성한다. 템플릿은 `.github/ISSUE_TEMPLATE/`(feature/bug/refactor/chore)을 사용하며, 제목에 `[FEAT]`, `[BUG]`, `[REFACTOR]`, `[CHORE]` 접두사를 붙인다.
2. `dev`에서 이슈 번호를 포함한 브랜치를 만든다: `feat/{issue-number}-{short-description}` 등. 자세한 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md) 참고.
3. 커밋은 [Conventional Commits](https://www.conventionalcommits.org/)(`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`, `build:`, `ci:`)를 따른다.
4. PR 생성 시 [.github/PULL_REQUEST_TEMPLATE.md](.github/PULL_REQUEST_TEMPLATE.md) 체크리스트를 채우고 관련 Issue를 연결한다.
5. PR을 열면 CI(`./gradlew clean build`)가 자동 실행된다.

## 민감정보 관리

- 실제 비밀번호, RDS 주소, API 키 등을 저장소에 커밋하지 않는다.
- `.env`는 `.gitignore`에 포함되어 있다. 환경변수 예시는 `.env.example`에만 이름을 남기고 실제 값을 넣지 않는다.
- prod 접속 정보는 항상 배포 환경변수로만 주입한다.

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
