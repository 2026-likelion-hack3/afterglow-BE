# 로컬 개발 환경 설정

## 1. JDK 설치 확인

Java 21이 필요하다 (`build.gradle`의 `sourceCompatibility`/`targetCompatibility` 기준).

```
java -version
```

`21.x.x`가 아니라면 Java 21(Temurin 등)을 설치하고 `JAVA_HOME`을 맞춘다.

## 2. PostgreSQL 준비

PostgreSQL을 준비하는 방법은 두 가지다. **반드시 둘 중 하나만 선택한다.**

> **포트 충돌 주의**: Windows에 PostgreSQL이 네이티브 서비스로 설치돼 있으면 기본 포트 `5432`를 그 서비스가 상시 점유한다. 그래서 Docker Compose PostgreSQL은 **호스트 포트 `5433`**을 기본값으로 쓰도록 분리되어 있다(컨테이너 내부 포트는 표준 `5432` 그대로, 호스트에 노출되는 포트만 다르다). `application-local.yml`도 기본값이 `localhost:${DB_PORT:5433}`이라 별도 설정 없이 `docker compose up -d` → 앱 실행만으로 항상 Docker PostgreSQL에 붙는다 — 네이티브 PostgreSQL이 떠 있어도 무시된다.
> - Windows: `Get-Service | Where-Object { $_.DisplayName -like "*PostgreSQL*" }` 로 네이티브 서비스가 실행 중인지 확인할 수 있다(있어도 상관없다, 이 저장소 로컬 개발은 항상 Docker Compose 쪽을 본다).
> - 네이티브 설치를 쓰기로 했다면(방법 B) `.env`의 `DB_PORT`를 `5432`로 바꾼다 — docker-compose의 컨테이너 노출 포트와 앱 접속 URL 양쪽이 이 변수 하나로 같이 바뀐다.

### 방법 A — Docker Compose (권장, 기본값)

저장소 루트의 `docker-compose.yml`은 PostgreSQL 컨테이너만 정의한다(애플리케이션 컨테이너나 Dockerfile은 없음).

1. `.env.example`을 복사해 `.env`를 만든다.
   ```
   copy .env.example .env      # Windows
   cp .env.example .env        # macOS/Linux
   ```
   기본값(`afterglow`/`afterglow`/호스트 포트 `5433`)을 그대로 써도 되고, 필요하면 `.env`에서 바꾼다. `.env`는 `.gitignore`에 포함되어 있어 커밋되지 않는다.

2. 컨테이너를 띄운다.
   ```
   docker compose up -d
   ```

3. 상태를 확인한다.
   ```
   docker compose ps
   docker compose logs -f postgres
   ```
   `STATUS`가 `healthy`가 되면(내부적으로 `pg_isready`로 확인) 준비된 것이다.

4. 종료할 때:
   ```
   docker compose down
   ```
   > **주의**: 데이터까지 삭제하려면 `docker compose down -v`를 사용한다. named volume(`afterglow-postgres-data`)이 함께 삭제되어 로컬 DB 데이터가 모두 사라지므로 신중히 실행한다.

### 방법 B — 로컬에 직접 설치된 PostgreSQL 사용

이미 PostgreSQL이 로컬에 설치·실행 중인 경우(Windows 서비스 `postgresql-x64-<버전>` 등)에 해당한다. 새로 설치할 필요는 없다.

1. PostgreSQL이 설치되어 있는지, 메이저 버전이 맞는지 확인한다(메이저 버전은 [README.md](../README.md#기술-스택) 참고). Windows에서 서비스로 설치된 경우:
   ```
   Get-Service | Where-Object { $_.DisplayName -like "*PostgreSQL*" }
   ```
2. **기존 계정/DB를 건드리지 않고** 프로젝트 전용 role/database가 이미 있는지 먼저 확인한다(관리자 계정으로 `psql` 접속 후):
   ```sql
   SELECT rolname FROM pg_roles WHERE rolname = 'afterglow';
   SELECT datname FROM pg_database WHERE datname = 'afterglow';
   ```
3. 둘 다 없을 때만 새로 만든다. 이미 존재한다면 비밀번호를 임의로 바꾸거나 DB를 덮어쓰지 않는다 — 다른 팀원/다른 프로젝트가 쓰고 있을 수 있다.
   ```sql
   CREATE USER afterglow WITH PASSWORD 'afterglow';
   CREATE DATABASE afterglow OWNER afterglow;
   ```
4. `application-local.yml`의 접속 정보는 기본값이 `localhost:${DB_PORT:5433}/afterglow`(계정 `afterglow`/`afterglow`)다. 네이티브 설치는 보통 표준 포트 `5432`를 쓰므로 환경변수 `DB_PORT=5432`로 덮어써야 한다. 다른 계정/비밀번호를 쓰려면 `DB_USERNAME`, `DB_PASSWORD`도 함께 덮어쓴다.
5. 기존에 설치된 PostgreSQL의 `pg_hba.conf` 인증 방식이 `scram-sha-256`(비밀번호 인증)인지 확인한다. 관리자(`postgres`) 계정 비밀번호를 모르면 1~3단계를 진행할 수 없으므로, 해당 계정을 관리하는 팀원/설치자에게 비밀번호를 받거나 직접 접속해 role/DB를 만들어달라고 요청한다.

## 3. 환경변수 설정

`.env.example`을 참고해 필요한 값을 설정한다. `local` 프로파일은 `DB_USERNAME`/`DB_PASSWORD`를 지정하지 않으면 `afterglow`/`afterglow` 기본값을 사용하므로, Docker Compose 기본값을 그대로 쓴다면 별도 설정 없이 실행할 수 있다.

**`JWT_SECRET`은 repository에 기본값이 없어 local에서도 반드시 직접 지정해야 한다** — 없으면 기동 자체가 실패한다. `openssl rand -base64 48`(또는 동등한 방법)로 생성한 값을 쓴다.

셸에서 직접 지정하려면:

```
# Windows (PowerShell)
$env:DB_USERNAME = "afterglow"
$env:DB_PASSWORD = "afterglow"
$env:JWT_SECRET = "<직접 생성한 값>"

# macOS/Linux
export DB_USERNAME=afterglow
export DB_PASSWORD=afterglow
export JWT_SECRET=<직접 생성한 값>
```

## 4. 애플리케이션 실행 (local 프로파일)

`application.yml`은 `spring.profiles.default: local`을 지정하므로, **활성 프로파일을 명시하지 않으면 자동으로 local이 적용**된다. 명시적으로 지정할 수도 있다.

**Windows**
```
:: 1) 기본 로컬 실행 (프로파일 미지정 → local이 기본 적용됨)
gradlew.bat bootRun

:: 2) local 프로파일을 명시하는 실행
gradlew.bat bootRun --args="--spring.profiles.active=local"
```

**macOS/Linux**
```
# 1) 기본 로컬 실행 (프로파일 미지정 → local이 기본 적용됨)
./gradlew bootRun

# 2) local 프로파일을 명시하는 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

두 방식 모두 결과는 동일하다. 명시적 실행은 CI나 다른 프로파일이 셸 환경변수로 이미 설정된 상태에서 로컬 실행을 확실히 하고 싶을 때 사용한다.

## 5. IntelliJ 실행 설정

1. `AfterglowBeApplication`을 Run Configuration으로 연다.
2. Environment variables에 필요 시 `DB_USERNAME`, `DB_PASSWORD`를 추가한다(기본값을 쓴다면 생략 가능). `JWT_SECRET`은 기본값이 없으므로 반드시 추가한다.
3. 활성 프로파일을 명시하고 싶다면 VM options에 `-Dspring.profiles.active=local`을 추가하거나, Program arguments에 `--spring.profiles.active=local`을 추가한다. 아무것도 지정하지 않아도 `spring.profiles.default: local`에 의해 local이 적용된다.
4. `SPRING_PROFILES_ACTIVE=prod`는 로컬 실행 설정에 넣지 않는다(운영 전용).

## 6. Gradle Wrapper 사용법

이 저장소는 Gradle 8.10 Wrapper를 사용한다. 로컬에 Gradle을 별도로 설치할 필요가 없다.

**Windows**
```
gradlew.bat clean build
gradlew.bat test
gradlew.bat bootRun
```

**macOS/Linux**
```
./gradlew clean build
./gradlew test
./gradlew bootRun
```

## 7. 빌드 실패 시 확인 사항

- `java -version`이 21인지 확인한다.
- `JAVA_HOME`이 JDK 21을 가리키는지 확인한다.
- Lombok 애노테이션 프로세싱이 IDE에서 활성화되어 있는지 확인한다(IntelliJ: Settings → Build → Compiler → Annotation Processors → Enable annotation processing).
- `gradlew.bat clean build` / `./gradlew clean build`를 다시 실행해 캐시 문제인지 확인한다.

## 8. DB 연결 실패 시 확인 사항

- PostgreSQL이 실행 중인지 확인한다.
  - Docker Compose 사용 시: `docker compose ps`로 `postgres` 서비스가 `healthy` 상태인지, 호스트 포트가 `5433`으로 매핑됐는지 확인한다.
  - 로컬 설치 사용 시: 서비스가 기동 중인지, 포트 5432가 열려 있는지 확인한다.
- 접속 정보(호스트/포트/DB명/계정/비밀번호)가 `application-local.yml`의 기본값(`DB_PORT` 기본 `5433`) 또는 설정한 환경변수와 일치하는지 확인한다.
- `.env`의 `DB_PORT`가 docker-compose 컨테이너 노출 포트와 앱 접속 URL 양쪽에 동시에 쓰인다. 단 `docker compose`는 `.env`를 자동으로 읽지만 `./gradlew bootRun`은 읽지 않으므로, 앱을 실행하는 셸(또는 IntelliJ 설정)에도 같은 `DB_PORT` 값을 export해야 한다 — `.env`만 바꾸고 셸에 반영하지 않으면 컨테이너와 앱이 다른 포트를 보게 된다.
- **스키마 관련 에러라면** — `ddl-auto: validate`이므로 Hibernate가 스키마를 만들지 않는다. Flyway가 기동 시 자동으로 `V1__baseline.sql`(과 이후 마이그레이션)을 실행하므로, 새로 만든 빈 DB인데도 실패한다면 Flyway 로그(`Migrating schema "public" to version "1 - baseline"`)가 실제로 찍히는지 먼저 확인한다.

## 9. Claude Code MCP 설정

저장소 루트의 `.mcp.json`에 Manyfast(기획 소스), Notion(팀 결정사항) MCP 서버가 project scope로 정의되어 있다. 둘 다 OAuth/로그인 방식이라 API Key나 환경변수 설정이 필요 없다.

Manyfast 연결:
1. Claude Code를 실행한다.
2. `/mcp` 명령을 입력한다.
3. `manyfast`를 선택한다.
4. `Authenticate`를 선택한다.
5. Manyfast 계정으로 로그인한다.
6. 연결할 그룹을 선택한다.

Notion도 같은 방식으로 `/mcp`에서 `notion`을 선택해 로그인한다.

인증 정보는 저장소에 저장되지 않으며, 각 개발자가 개별적으로 로그인한다.
- Figma는 `.mcp.json`에 없다 — 공식 Claude Code plugin(`claude plugin install figma@claude-plugins-official`)을 각자 설치하고 OAuth 로그인하는 방식으로 쓴다.
