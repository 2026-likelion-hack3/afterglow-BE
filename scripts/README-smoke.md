# Episode 핵심 플로우 Smoke Test

`scripts/smoke-episode-flow.sh` — 배포 직후 백엔드 API가 실제로 살아있고 핵심 흐름이 이어지는지
빠르게 확인하는 스크립트다. **브라우저 CORS 검증용이 아니라 백엔드 API E2E 검증용**이다(curl에는
CORS가 적용되지 않는다 — CORS는 이 문서 마지막 절 참고).

## 1. 사전 요구사항

- `curl`
- `python3` — 표준 라이브러리(`json`)만 사용한다. 별도 패키지 설치가 필요 없다.
  - JSON 응답 파싱(`accessToken`/`episodeId`/`hold`/`cards` 길이/`startDate`/`verdict` 추출)과
    Day2/Day3 날짜 계산에 쓴다. 날짜 계산에 OS `date` 명령을 쓰지 않는 이유: macOS는 BSD `date`,
    Linux는 GNU `date`라 `-d`/`-v` 옵션이 서로 달라 깨지기 쉽다 — `python3`로 통일해 이 문제를
    피한다.
- 위 둘 중 하나라도 없으면 스크립트가 시작하자마자 명확한 에러 메시지와 함께 종료한다. `jq` 등
  추가 설치가 필요한 도구는 쓰지 않는다.

## 2. 로컬 실행 명령

```bash
./scripts/smoke-episode-flow.sh
```

`API_BASE` 기본값은 `http://localhost:8080`이다. 로컬 서버(`./gradlew bootRun` 등)가 그 주소에서
떠 있어야 한다.

## 3. EC2(배포 서버) 실행 명령

**8080이 인바운드로 열려 있는 경우(직접 호출)** — 실제 EC2 IP로 바로 지정한다:

```bash
API_BASE=http://3.34.180.12:8080 ./scripts/smoke-episode-flow.sh
```

(IP는 재배포/재시작 시 바뀔 수 있으니 매번 콘솔에서 최신 값을 확인한다.)

**8080이 인바운드로 안 열려 있는 경우(기본값, docs/deployment.md 정책)** — SSH tunnel을 먼저 연
뒤 tunnel의 로컬 포트를 `API_BASE`로 지정한다:

```bash
# 별도 터미널에서 tunnel을 열어둔다
ssh -i <key.pem> -L 8081:localhost:8080 ec2-user@<EC2_IP>

# 이 스크립트는 tunnel의 로컬 포트를 향한다
API_BASE=http://localhost:8081 ./scripts/smoke-episode-flow.sh
```

또는 EC2 인스턴스에 직접 SSH로 들어가서 로컬(`localhost:8080`)로 실행해도 된다:

```bash
ssh -i <key.pem> ec2-user@<EC2_IP>
# EC2 내부에서
API_BASE=http://localhost:8080 ./scripts/smoke-episode-flow.sh
```

## 4. 성공했을 때 예상 출력

각 단계마다 `==> [N] <API 경로>`로 진행 상황을 보여주고, 단계별로 `[OK] ... — HTTP <code>`가 출력된다.
마지막은 다음과 같다:

```
==> [8] GET /api/episodes/123/routine/day3-result
[OK] day3 결과 조회 — HTTP 200
verdict=STOP
[OK] Day3 판정 = STOP (Day2의 WORSE가 실제로 반영됨 — Day3DecisionEngine이 3일 전체를 소비함)

==================================================
 전체 플로우 성공 (episodeId=123)
==================================================
```

5단계(analysis)에서는 Vanity/Tracking이 아직 실제 연동되지 않은 상태라면 `hold=true`,
`holdReason=NO_TARGET`이 나오는 게 **정상**이다(실패 아님) — 스크립트도 이 경우를 실패로 처리하지
않고 그대로 안내 메시지만 출력한다.

## 5. 실패했을 때 가장 먼저 볼 항목

- **CORS 오류처럼 보이는 메시지**: curl에는 CORS가 적용되지 않으므로, curl 응답 자체가 CORS 때문에
  실패하는 일은 없다. CORS는 브라우저 전용 정책이다 — 이 스크립트가 실패했다면 CORS는 원인이 아니다.
- **401 Unauthorized**: `accessToken`이 정상 발급됐는지(`1단계` 출력의 `accessToken=...` 확인), 또는
  `Authorization: Bearer <token>` 헤더가 실제로 붙었는지 확인한다.
- **409 Conflict**: Episode 상태 전이 순서를 확인한다 — intake 전 analysis, analysis 전 routine은
  모두 409가 정상이다. 같은 episode에 routine을 두 번 만들려 해도 409다. 스크립트가 매번 새 episode를
  만들기 때문에 정상 흐름에서 409가 나온다면 서버의 상태 전이 로직이나 API_BASE가 예상과 다른 서버를
  가리키고 있는지 의심한다.
- **500 Internal Server Error**: 서버 로그를 확인한다(로컬은 콘솔, EC2는 `docker logs -f afterglow`).
  Vanity/Tracking이 아직 실제 연동되지 않은 상태라 관련 오류는 아니어야 한다(연동 전 raw data 조회는
  전부 "대상 없음"만 반환하도록 격리돼 있다).
- **연결 자체가 안 됨(`curl: (7) Failed to connect`)**: `API_BASE`가 맞는지, 서버가 실제로 떠 있는지,
  EC2라면 tunnel이 열려 있는지 확인한다.

## 6. CORS preflight 확인 (참고, 이 smoke test와 별개)

이 smoke test 스크립트는 curl 기반이라 CORS를 검증하지 않는다. 브라우저 preflight를 별도로 사람이
확인하려면 아래 명령을 쓴다(응답 헤더를 눈으로 확인):

```bash
curl -i -X OPTIONS "${API_BASE:-http://localhost:8080}/api/onboarding" \
  -H "Origin: http://localhost:8081" \
  -H "Access-Control-Request-Method: PUT" \
  -H "Access-Control-Request-Headers: authorization,content-type"
```

확인할 것:
- 상태 코드가 2xx
- 응답 헤더에 `Access-Control-Allow-Origin: http://localhost:8081`
- `Access-Control-Allow-Methods`에 `PUT` 포함
- `Access-Control-Allow-Headers`에 `authorization`, `content-type` 포함
