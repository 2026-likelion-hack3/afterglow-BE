# Deployment

## prod 프로파일 활성화

`SPRING_PROFILES_ACTIVE=prod`와 함께 아래 환경변수를 주입한다. **prod 프로파일은 소스에서 기본값으로 지정하지 않으며, 반드시 외부 환경변수로 명시해야 활성화된다.**

## 환경변수 목록

`.env.example`을 복사해 `.env`로 사용한다(`.env`는 git에 커밋하지 않는다). 로컬 개발 환경 설정은 [local-setup.md](local-setup.md) 참고.

| 변수 | 용도 | 비고 |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | docker-compose.yml의 로컬 PostgreSQL 계정/DB명 | 기본값 `afterglow`/`afterglow`/`afterglow` |
| `DB_USERNAME`, `DB_PASSWORD` | local 프로파일 DB 접속 계정 | 미지정 시 기본값 `afterglow`/`afterglow` |
| `DB_PORT` | docker-compose 컨테이너 노출 포트 **+** local 프로파일 DB 접속 포트(하나의 변수를 양쪽이 같이 읽음) | 미지정 시 기본값 `5433`(호스트 — Windows 네이티브 PostgreSQL의 기본 `5432`와 충돌 방지, 컨테이너 내부는 표준 `5432` 그대로). CI는 `5432`로 override. `docker compose`는 `.env`를 자동으로 읽지만 앱 실행(`bootRun` 등)은 읽지 않으므로 셸에도 동일하게 export해야 함 |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | prod 프로파일 DB 접속 정보 | **prod에서는 필수**, 실제 값은 배포 환경변수로만 주입 |
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 | prod 배포 시 `prod`로 반드시 지정 |
| `JWT_SECRET`, `JWT_EXPIRATION_SECONDS` | JWT 서명 키·만료 시간 | local은 기본값 사용 가능, prod는 반드시 배포 환경변수로 지정. **`JWT_SECRET`은 최소 32바이트(256bit) 이상이어야 함** — 짧으면 HS 알고리즘 키 생성 단계(`JwtTokenProvider`)에서 기동 자체가 실패한다(`openssl rand -base64 48` 등으로 생성 권장) |
| `AWS_REGION`, `SES_SENDER_EMAIL` | prod 프로파일에서 SES 이메일 발송 설정 | **prod에서는 필수**. AWS 자격증명 자체는 EC2 IAM role의 기본 자격증명 체인을 사용하므로 별도 키는 필요 없음 |

## AWS 연동

AWS(SES) 연동은 EC2 인스턴스 IAM role의 기본 자격증명 체인을 사용하며, 별도 Access Key/Secret Key를 설정 파일이나 환경변수에 넣지 않는다.

## Docker 이미지 빌드

애플리케이션 컨테이너화는 `Dockerfile`(multi-stage build)로 구성되어 있다.

1. Gradle build stage(`eclipse-temurin:21-jdk-jammy`)에서 `./gradlew clean bootJar -x test`로 실행 가능한 jar를 만든다. 테스트는 실제 PostgreSQL에 의존(`@SpringBootTest`)해 이 단계에서 DB에 접근할 수 없으므로 여기서는 실행하지 않는다 — 테스트 검증은 CI의 Gradle 단계(아래 참고)가 전담한다.
2. 최소 runtime stage(`eclipse-temurin:21-jre-jammy`)로 jar만 복사해 non-root 사용자(`spring`)로 실행한다.
3. Spring profile, DB 접속 정보, JWT/AWS 설정 등은 이미지에 baking하지 않고 컨테이너 실행 시 환경변수로 주입한다.

로컬 빌드/실행:
```
docker build -t afterglow-be:local .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5433/afterglow \
  -e DB_USERNAME=afterglow \
  -e DB_PASSWORD=afterglow \
  afterglow-be:local
```
`docker-compose.yml`로 띄운 PostgreSQL은 호스트 포트(`5433`)로 노출되어 있으므로, 애플리케이션 컨테이너에서는 `--network host` 대신 Docker Desktop이 제공하는 `host.docker.internal`로 접근한다. `application-local.yml`은 `localhost:${DB_PORT:5433}`을 기본으로 쓰므로, 이 예시처럼 컨테이너에서 실행할 땐 `SPRING_DATASOURCE_URL` 환경변수로 접속 URL을 덮어쓴다.

## AWS 인프라 구성 (EC2 + RDS 수동 배포)

CD(자동 배포)는 아직 없고, 아래 구성으로 **1회 수동 배포·스모크 테스트까지 완료**된 상태다. 리소스 실제 식별자(인스턴스 ID, endpoint, IP 등)는 민감정보 관리 원칙에 따라 이 문서에 남기지 않는다 — AWS 콘솔에서 확인한다.

- **EC2**: Amazon Linux 2023, Docker Engine(`dnf install docker`)으로 컨테이너 실행. `SPRING_PROFILES_ACTIVE=prod` 등 환경변수는 `--env-file`로 컨테이너 실행 시점에 주입(이미지에 baking 안 함, 위 "Docker 이미지 빌드" 절과 동일 원칙). 퍼블릭 IP는 있지만 **8080은 인바운드로 열지 않는다** — 외부 확인이 필요하면 SSH tunnel(`ssh -L <local-port>:localhost:8080 ec2-user@<EC2-IP>`)을 사용한다. SSH(22)만 관리자 IP `/32`로 제한해서 연다.
  - **IMDS 설정 주의**: IMDSv2 강제(`HttpTokens=required`) + `HttpPutResponseHopLimit=2`로 설정해야 한다. 애플리케이션이 Docker 컨테이너 내부에서 `DefaultCredentialsProvider`로 IMDS를 호출하는데, 컨테이너 브리지 네트워크를 거치며 홉이 하나 늘어나므로 기본값(1)이면 자격증명 조회가 실패한다.
- **RDS**: PostgreSQL(관리형), **Publicly Accessible = No**. EC2에서만 접근 가능하도록 RDS의 Security Group 인바운드 소스를 EC2 Security Group으로 참조한다(CIDR 아님). 인터넷에 5432를 여는 방식은 쓰지 않는다.
- **IAM**: EC2에 Instance Profile(Role)을 연결해 SES 자격증명을 처리한다. Access Key/Secret Key는 어디에도 저장하지 않는다(위 "AWS 연동" 절과 동일). SES `SendEmail`/`SendRawEmail` 권한은 `Resource: "*"` + `Condition: ses:FromAddress = <발신 identity>` 형태로 제한한다 — `Resource`를 발신 identity ARN 하나로만 좁히면 IAM 인가 단계에서 실패하므로 이 조건부 패턴을 쓴다.
- **스키마**: Flyway로 관리한다(`src/main/resources/db/migration/`). local/prod 둘 다 `ddl-auto: validate`이며, Hibernate는 스키마를 만들지 않고 Flyway가 만든 스키마와 Entity 매핑이 일치하는지 검증만 한다. 신규/빈 DB는 앱 기동 시 Flyway가 `V1__baseline.sql`부터 순서대로 자동 실행한다. 이미 Flyway 도입 이전 방식(`ddl-auto=update` 1회성 override)으로 스키마가 만들어져 있던 기존 RDS는, Flyway를 처음 붙이는 시점에만 `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` + `SPRING_FLYWAY_BASELINE_VERSION=1` 환경변수로 "V1이 이미 적용된 것"으로 편입(baseline)한 뒤, 이 override를 즉시 제거하고 정상 상태(override 없음)로 재기동해 검증한다 — 이 두 환경변수를 `application-prod.yml`이나 배포용 env 파일에 영구 저장하지 않는다. 이후 스키마 변경은 전부 `V2`, `V3`... 형태의 새 migration 파일로만 한다.
- **SES**: 현재 계정이 **sandbox** 상태라 verified된 수신자에게만 실제 발송된다. Production access 신청은 별도 작업.
- **인증**: 현재는 AWS root 계정으로 리소스를 관리한다. IAM Identity Center 기반 관리자 identity 분리는 후속 보안 개선 작업으로 남아 있다.

## 현재 상태

- **Docker image 빌드까지 구현됨.** 이미지를 registry(GHCR 등)에 push하는 단계와 CD(자동 배포)는 아직 없다 — 현재는 로컬에서 빌드한 이미지를 `docker save`/`scp`/`docker load`로 EC2에 수동 전달한다.
- CI(GitHub Actions)는 `dev`/`main` 대상 PR에서 (1) Gradle build/test, (2) Docker image build(push 없이 빌드 검증만)까지 실행한다. 이미지를 실제 PostgreSQL과 함께 띄워보는 integration/smoke test는 아직 CI에 없다(로컬/AWS 수동 검증으로만 확인).
- **AWS EC2/RDS 수동 배포 및 스모크 테스트 완료**: health check, Account/Episode API, 실제 SES 이메일 발송까지 전부 정상 동작을 확인했다.
- **Flyway 도입 완료(local/CI 검증 기준)** — 기존 RDS에 대한 실제 baseline 편입은 아직 진행 전이다(별도 단계). RDS/EC2 프로비저닝 자동화(IaC), GHCR push, CD 파이프라인, SES production access, IAM Identity Center 전환은 이후 별도 작업으로 진행한다.

## 민감정보 관리

- 실제 비밀번호, RDS 주소, API 키 등을 저장소에 커밋하지 않는다.
- `.env`는 `.gitignore`에 포함되어 있다. 환경변수 예시는 `.env.example`에만 이름을 남기고 실제 값을 넣지 않는다.
- prod 접속 정보는 항상 배포 환경변수로만 주입한다.
- prod용 실제 값은 저장소 밖(예: 로컬 사용자 홈 디렉터리)에 별도 env 파일로 보관하고, EC2로 전달할 때도 이미지에 baking하지 않고 파일 전송 후 `--env-file`로만 주입한다. `.env`(로컬 개발용)와는 분리해서 관리한다.
