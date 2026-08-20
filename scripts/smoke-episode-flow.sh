#!/usr/bin/env bash
#
# Episode 핵심 플로우 smoke test — 배포 후 백엔드 API를 빠르게 E2E로 검증한다.
# 브라우저 CORS 검증용이 아니다(curl에는 CORS가 적용되지 않는다) — 별도 CORS 확인은
# scripts/README-smoke.md 마지막 절의 curl 명령을 참고한다.
#
# 사용법:
#   API_BASE=http://localhost:8080 ./scripts/smoke-episode-flow.sh   (기본값)
#   API_BASE=http://3.34.180.12:8080 ./scripts/smoke-episode-flow.sh
#
# 흐름: anonymous 계정 -> onboarding -> episode 생성 -> intake -> analysis 실행
#       -> routine 시작(Day1~3) -> Day1/2/3 check-in -> Day3 판정 조회
#
# Day2에 WORSE를 넣어 Day3JudgmentEngine이 "3일차 응답만" 보는 게 아니라 Day1~3
# 전부를 실제로 반영하는지 확인한다(WORSE 규칙 적용 시 최종 판정은 STOP이어야 한다).
#
# 의존성: curl, python3(표준 라이브러리 json만 사용) — 별도 패키지 설치가 필요한
# 도구(jq 등)는 쓰지 않는다.

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"

# ---------- 사전 점검 ----------
if ! command -v curl >/dev/null 2>&1; then
	echo "[ERROR] curl이 필요합니다." >&2
	exit 1
fi
if ! command -v python3 >/dev/null 2>&1; then
	echo "[ERROR] python3가 필요합니다(JSON 파싱 및 Day2/Day3 날짜 계산에 사용 — 표준 라이브러리만 쓰므로 별도 설치는 필요 없습니다)." >&2
	exit 1
fi

BODY_FILE="$(mktemp)"
trap 'rm -f "$BODY_FILE"' EXIT

step() {
	echo
	echo "==> [$1] $2"
}

# request METHOD PATH [BODY_JSON] [TOKEN]
# 응답 본문은 $BODY_FILE에 저장하고, HTTP status를 stdout으로 출력한다.
request() {
	local method="$1" path="$2" body="${3:-}" token="${4:-}"
	local -a args=(-sS -o "$BODY_FILE" -w '%{http_code}' -X "$method" "${API_BASE}${path}" -H "Content-Type: application/json")
	if [ -n "$token" ]; then
		args+=(-H "Authorization: Bearer ${token}")
	fi
	if [ -n "$body" ]; then
		args+=(-d "$body")
	fi
	curl "${args[@]}"
}

# expect_status ACTUAL EXPECTED LABEL — 불일치 시 응답 본문을 보여주고 즉시 종료한다.
expect_status() {
	local actual="$1" expected="$2" label="$3"
	if [ "$actual" != "$expected" ]; then
		echo "[FAIL] ${label} — expected HTTP ${expected}, got HTTP ${actual}" >&2
		echo "---- response body ----" >&2
		cat "$BODY_FILE" >&2
		echo >&2
		exit 1
	fi
	echo "[OK] ${label} — HTTP ${actual}"
}

# json_field KEY — $BODY_FILE의 최상위 필드 하나를 꺼낸다. 값이 없으면 빈 문자열, boolean은 true/false로 출력한다.
json_field() {
	python3 -c "
import json, sys
with open(sys.argv[1], encoding='utf-8') as f:
    data = json.load(f)
value = data.get(sys.argv[2])
if isinstance(value, bool):
    print(str(value).lower())
elif value is None:
    print('')
else:
    print(value)
" "$BODY_FILE" "$1"
}

# json_array_len KEY — $BODY_FILE의 최상위 배열 필드 길이를 꺼낸다.
json_array_len() {
	python3 -c "
import json, sys
with open(sys.argv[1], encoding='utf-8') as f:
    data = json.load(f)
print(len(data.get(sys.argv[2]) or []))
" "$BODY_FILE" "$1"
}

# print_body — $BODY_FILE을 사람이 읽기 좋게 pretty-print한다.
print_body() {
	python3 -m json.tool "$BODY_FILE"
}

echo "API_BASE=${API_BASE}"

# ---------- 1. anonymous 계정 생성 ----------
step 1 "POST /api/accounts/anonymous"
STATUS=$(request POST "/api/accounts/anonymous")
expect_status "$STATUS" 200 "anonymous 계정 생성"
ACCESS_TOKEN=$(json_field accessToken)
if [ -z "$ACCESS_TOKEN" ]; then
	echo "[FAIL] 응답에서 accessToken을 찾을 수 없습니다." >&2
	cat "$BODY_FILE" >&2
	exit 1
fi
echo "accessToken=${ACCESS_TOKEN:0:16}...(masked)"

# ---------- 2. onboarding ----------
step 2 "PUT /api/onboarding"
ONBOARDING_BODY='{"ageRange":"FIFTY_TO_FIFTY_FOUR","menstrualStatus":"IRREGULAR"}'
STATUS=$(request PUT "/api/onboarding" "$ONBOARDING_BODY" "$ACCESS_TOKEN")
case "$STATUS" in
	2??) echo "[OK] onboarding 완료 — HTTP ${STATUS}" ;;
	*)
		echo "[FAIL] onboarding 완료 — expected 2xx, got HTTP ${STATUS}" >&2
		cat "$BODY_FILE" >&2
		exit 1
		;;
esac
print_body

# ---------- 3. episode 생성 ----------
step 3 "POST /api/episodes"
SYMPTOM_BODY='{"angle":90.0,"radius":0.5,"primarySymptom":"REDNESS","severity":"MODERATE"}'
STATUS=$(request POST "/api/episodes" "$SYMPTOM_BODY" "$ACCESS_TOKEN")
expect_status "$STATUS" 200 "episode 생성"
EPISODE_ID=$(json_field episodeId)
if [ -z "$EPISODE_ID" ]; then
	echo "[FAIL] 응답에서 episodeId를 찾을 수 없습니다." >&2
	cat "$BODY_FILE" >&2
	exit 1
fi
echo "episodeId=${EPISODE_ID}"

# ---------- 4. intake 제출 ----------
step 4 "POST /api/episodes/${EPISODE_ID}/intake"
INTAKE_BODY='{"onsetPeriod":"TODAY","bodyParts":["CHEEK"],"recentNewProductName":"new serum","notes":null}'
STATUS=$(request POST "/api/episodes/${EPISODE_ID}/intake" "$INTAKE_BODY" "$ACCESS_TOKEN")
expect_status "$STATUS" 204 "intake 제출"

# ---------- 5. analysis 실행 ----------
step 5 "POST /api/episodes/${EPISODE_ID}/analysis"
STATUS=$(request POST "/api/episodes/${EPISODE_ID}/analysis" "" "$ACCESS_TOKEN")
expect_status "$STATUS" 200 "analysis 실행"
CARD_COUNT=$(json_array_len cards)
if [ "$CARD_COUNT" != "3" ]; then
	echo "[FAIL] cards 개수가 3이 아닙니다: ${CARD_COUNT}" >&2
	cat "$BODY_FILE" >&2
	exit 1
fi
HOLD=$(json_field hold)
HOLD_REASON=$(json_field holdReason)
echo "[OK] analysis 실행 — cards=3, hold=${HOLD}, holdReason=${HOLD_REASON}"
if [ "$HOLD" = "true" ] && [ "$HOLD_REASON" = "NO_TARGET" ]; then
	echo "    (Vanity/Tracking이 아직 실제 연동되지 않은 상태에서는 HOLD(NO_TARGET)가 정상입니다 — 실패 아님.)"
fi
print_body

# ---------- 6. routine 시작 ----------
step 6 "POST /api/episodes/${EPISODE_ID}/routine"
ROUTINE_BODY='{"items":[
	{"usage":"CONTINUE","dayNumber":1,"timeSlot":"MORNING","productId":100},
	{"usage":"CONTINUE","dayNumber":2,"timeSlot":"MORNING","productId":100},
	{"usage":"CONTINUE","dayNumber":3,"timeSlot":"MORNING","productId":100},
	{"usage":"DISCONTINUE","dayNumber":null,"timeSlot":null,"productId":999}
]}'
STATUS=$(request POST "/api/episodes/${EPISODE_ID}/routine" "$ROUTINE_BODY" "$ACCESS_TOKEN")
expect_status "$STATUS" 201 "routine 시작"
START_DATE=$(json_field startDate)
DAY_COUNT=$(json_array_len days)
if [ -z "$START_DATE" ]; then
	echo "[FAIL] 응답에서 startDate를 찾을 수 없습니다." >&2
	cat "$BODY_FILE" >&2
	exit 1
fi
if [ "$DAY_COUNT" != "3" ]; then
	echo "[FAIL] routine days 개수가 3이 아닙니다(Day1/2/3 확인 실패): ${DAY_COUNT}" >&2
	cat "$BODY_FILE" >&2
	exit 1
fi
echo "[OK] routine 시작 — startDate=${START_DATE}, days=3(Day1~Day3 확인됨)"
print_body

DAY2_DATE=$(python3 -c "import sys, datetime; print(datetime.date.fromisoformat(sys.argv[1]) + datetime.timedelta(days=1))" "$START_DATE")
DAY3_DATE=$(python3 -c "import sys, datetime; print(datetime.date.fromisoformat(sys.argv[1]) + datetime.timedelta(days=2))" "$START_DATE")
echo "Day1=${START_DATE}  Day2=${DAY2_DATE}  Day3=${DAY3_DATE}"

# ---------- 7. Day1/Day2/Day3 check-in ----------
step 7 "PUT /api/episodes/${EPISODE_ID}/check-ins/{date} x3"
STATUS=$(request PUT "/api/episodes/${EPISODE_ID}/check-ins/${START_DATE}" '{"status":"IMPROVED"}' "$ACCESS_TOKEN")
expect_status "$STATUS" 200 "Day1(${START_DATE}) check-in = IMPROVED"

STATUS=$(request PUT "/api/episodes/${EPISODE_ID}/check-ins/${DAY2_DATE}" '{"status":"WORSE"}' "$ACCESS_TOKEN")
expect_status "$STATUS" 200 "Day2(${DAY2_DATE}) check-in = WORSE"

STATUS=$(request PUT "/api/episodes/${EPISODE_ID}/check-ins/${DAY3_DATE}" '{"status":"IMPROVED"}' "$ACCESS_TOKEN")
expect_status "$STATUS" 200 "Day3(${DAY3_DATE}) check-in = IMPROVED"

# ---------- 8. Day3 판정 조회 ----------
step 8 "GET /api/episodes/${EPISODE_ID}/routine/day3-result"
STATUS=$(request GET "/api/episodes/${EPISODE_ID}/routine/day3-result" "" "$ACCESS_TOKEN")
expect_status "$STATUS" 200 "day3 결과 조회"
VERDICT=$(json_field verdict)
echo "verdict=${VERDICT}"
if [ "$VERDICT" != "STOP" ]; then
	echo "[FAIL] 예상 판정(STOP, Day2 WORSE 규칙)과 다릅니다: ${VERDICT}" >&2
	cat "$BODY_FILE" >&2
	exit 1
fi
echo "[OK] Day3 판정 = STOP (Day2의 WORSE가 실제로 반영됨 — Day3DecisionEngine이 3일 전체를 소비함)"

echo
echo "=================================================="
echo " 전체 플로우 성공 (episodeId=${EPISODE_ID})"
echo "=================================================="
