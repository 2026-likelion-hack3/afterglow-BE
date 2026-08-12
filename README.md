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

자세한 절차와 트러블슈팅은 [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md)를 참고한다.

## 환경변수 목록

`.env.example`을 복사해 `.env`로 사용한다(`.env`는 git에 커밋하지 않는다).

| 변수 | 용도 | 비고 |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | docker-compose.yml의 로컬 PostgreSQL 설정 | 기본값 `afterglow`/`afterglow`/`afterglow`/`5432` |
| `DB_USERNAME`, `DB_PASSWORD` | local 프로파일 DB 접속 계정 | 미지정 시 기본값 `afterglow`/`afterglow` |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | prod 프로파일 DB 접속 정보 | **prod에서는 필수**, 실제 값은 배포 환경변수로만 주입 |
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 | prod 배포 시 `prod`로 반드시 지정 |
| `JWT_SECRET`, `JWT_EXPIRATION_SECONDS` | JWT 서명 키·만료 시간 | local은 기본값 사용 가능, prod는 반드시 배포 환경변수로 지정 |
| `AWS_REGION`, `SES_SENDER_EMAIL` | prod 프로파일에서 SES 이메일 발송 설정 | **prod에서는 필수**. AWS 자격증명 자체는 EC2 IAM role의 기본 자격증명 체인을 사용하므로 별도 키는 필요 없음 |

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

`SPRING_PROFILES_ACTIVE=prod`와 함께 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `AWS_REGION`, `SES_SENDER_EMAIL`을 환경변수로 주입한다. **prod 프로파일은 소스에서 기본값으로 지정하지 않으며, 반드시 외부 환경변수로 명시해야 활성화된다.** AWS(SES) 연동은 EC2 인스턴스 IAM role의 기본 자격증명 체인을 사용하며, 별도 Access Key/Secret Key를 설정 파일이나 환경변수에 넣지 않는다.

## 패키지 구조

기능명세서의 장(章) 구성을 그대로 따른다. 각 패키지의 관계와 설계 배경은 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)에 정리되어 있다.

- `onboarding` — 온보딩(1장)
- `episode` — 증상 접수부터 3일차 판정까지(2~4장): `intake`, `analysis`, `card`, `routine`, `checkin`
- `vanity` — 화장대(5장) — 현재 `Product`, `CombinationRule` 엔티티와 관련 enum만 존재
- `tracking` — 일상 기록·주간 리포트(6장)
- `trust` — 상업적 중립성·금지 용어·사진 처리 정책(7장)
- `notification` — 알림(8장)
- `account` — 계정·데이터 관리(9장)
- `story` — 커뮤니티(10장)
- `common` — 베이스 엔티티, 공통 예외 처리, JPA Auditing 설정

## GitHub Issue 및 PR 작업 흐름

1. 작업 전 Issue를 생성한다. 템플릿은 `.github/ISSUE_TEMPLATE/`(feature/bug/refactor/chore)을 사용하며, 제목에 `[FEAT]`, `[BUG]`, `[REFACTOR]`, `[CHORE]` 접두사를 붙인다.
2. 이슈 번호를 포함한 브랜치를 만든다: `feat/{issue-number}-{short-description}` 등. 자세한 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md) 참고.
3. 커밋은 [Conventional Commits](https://www.conventionalcommits.org/)(`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`, `build:`, `ci:`)를 따른다.
4. PR 생성 시 [.github/PULL_REQUEST_TEMPLATE.md](.github/PULL_REQUEST_TEMPLATE.md) 체크리스트를 채우고 관련 Issue를 연결한다.
5. PR을 열면 CI(`./gradlew clean build`)가 자동 실행된다.

## 민감정보 관리

- 실제 비밀번호, RDS 주소, API 키 등을 저장소에 커밋하지 않는다.
- `.env`는 `.gitignore`에 포함되어 있다. 환경변수 예시는 `.env.example`에만 이름을 남기고 실제 값을 넣지 않는다.
- prod 접속 정보는 항상 배포 환경변수로만 주입한다.

## 더 읽어보기

- [CONTRIBUTING.md](CONTRIBUTING.md) — 브랜치/커밋/PR 규칙
- [docs/CONVENTIONS.md](docs/CONVENTIONS.md) — 코드 컨벤션
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — 아키텍처, 데이터 저장 경계, 얼굴 사진 처리 원칙
- [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md) — 로컬 개발 환경 설정, 트러블슈팅
