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

## 현재 상태

- Dockerfile 없음 — 로컬 PostgreSQL 실행용 `docker-compose.yml`만 존재, 애플리케이션 컨테이너화는 아직 하지 않았다.
- 실제 배포 자동화 파이프라인 없음 — 현재 GitHub Actions는 PR에서 `./gradlew clean build`(빌드+테스트) 실행까지만 한다.
- 실제 AWS EC2/RDS에 배포된 적은 아직 없다(로컬 환경에서 prod 프로파일 + 실제 AWS SES로 수동 검증만 완료된 상태).

## 민감정보 관리

- 실제 비밀번호, RDS 주소, API 키 등을 저장소에 커밋하지 않는다.
- `.env`는 `.gitignore`에 포함되어 있다. 환경변수 예시는 `.env.example`에만 이름을 남기고 실제 값을 넣지 않는다.
- prod 접속 정보는 항상 배포 환경변수로만 주입한다.
