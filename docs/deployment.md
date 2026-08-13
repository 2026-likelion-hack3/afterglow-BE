# Deployment

## prod 프로파일 활성화

`SPRING_PROFILES_ACTIVE=prod`와 함께 아래 환경변수를 주입한다. **prod 프로파일은 소스에서 기본값으로 지정하지 않으며, 반드시 외부 환경변수로 명시해야 활성화된다.**

## 환경변수 목록

`.env.example`을 복사해 `.env`로 사용한다(`.env`는 git에 커밋하지 않는다). 로컬 개발 환경 설정은 [local-setup.md](local-setup.md) 참고.

| 변수 | 용도 | 비고 |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | docker-compose.yml의 로컬 PostgreSQL 설정 | 기본값 `afterglow`/`afterglow`/`afterglow`/`5432` |
| `DB_USERNAME`, `DB_PASSWORD` | local 프로파일 DB 접속 계정 | 미지정 시 기본값 `afterglow`/`afterglow` |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | prod 프로파일 DB 접속 정보 | **prod에서는 필수**, 실제 값은 배포 환경변수로만 주입 |
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 | prod 배포 시 `prod`로 반드시 지정 |
| `JWT_SECRET`, `JWT_EXPIRATION_SECONDS` | JWT 서명 키·만료 시간 | local은 기본값 사용 가능, prod는 반드시 배포 환경변수로 지정 |
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
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/afterglow \
  -e DB_USERNAME=afterglow \
  -e DB_PASSWORD=afterglow \
  afterglow-be:local
```
`docker-compose.yml`로 띄운 PostgreSQL은 호스트 포트로 노출되어 있으므로, 애플리케이션 컨테이너에서는 `--network host` 대신 Docker Desktop이 제공하는 `host.docker.internal`로 접근한다. `application-local.yml`은 `localhost:5432`를 고정 사용하므로 `SPRING_DATASOURCE_URL` 환경변수로 접속 URL을 덮어쓴다.

## 현재 상태

- **Docker image 빌드까지 구현됨.** 다만 이미지를 registry(GHCR 등)에 push하는 단계와 CD(자동 배포)는 아직 없다.
- CI(GitHub Actions)는 `dev`/`main` 대상 PR에서 (1) Gradle build/test, (2) Docker image build(push 없이 빌드 검증만)까지 실행한다. 이미지를 실제 PostgreSQL과 함께 띄워보는 integration/smoke test는 아직 없다.
- 실제 AWS EC2/RDS에 배포된 적은 아직 없다(로컬 환경에서 prod 프로파일 + 실제 AWS SES로 수동 검증만 완료된 상태). RDS/EC2 프로비저닝과 GHCR push, CD 파이프라인은 이후 별도 작업으로 진행한다.

## 민감정보 관리

- 실제 비밀번호, RDS 주소, API 키 등을 저장소에 커밋하지 않는다.
- `.env`는 `.gitignore`에 포함되어 있다. 환경변수 예시는 `.env.example`에만 이름을 남기고 실제 값을 넣지 않는다.
- prod 접속 정보는 항상 배포 환경변수로만 주입한다.
