# Episode

## Responsibility

사건 루프 — 증상 접수부터 3일차 판정까지 하나의 에피소드로 묶이는 흐름. 기능명세서 2~4장.

## Current Status

서브도메인별로 상태가 다르다.

| 서브도메인 | 명세 범위 | 상태 |
|---|---|---|
| `intake` | 2.1~2.3 (사진 촬영 제외) | **구현 완료** |
| `analysis` | 2.4 통합 분석 | 기획 확정(원인 후보 4종, 확신 단계 판정 기준, evidence 구조 일부) / 후보별 score 계산·후보 생성 조건 확인 중 / 코드 구현 보류 |
| `card` | 3.1, 4.3 | analysis 의존성 및 화면 요구사항 일부 확정 / 미구현 |
| `routine` | 3.2 | 미구현 |
| `checkin` | 4.1~4.2 | 미구현 |

## Planned Features

- **card**: 결과 카드 3장(중단/사용/병원 권유), 빈 범주 안내
- **routine**: 3일 루틴 생성
- **checkin**: 일일 1탭 체크, 3일차 판정 분기

세 서브도메인 모두 analysis 결과가 있어야 진행 가능해, analysis 구현 전까지는 착수하지 않는다.

## Domain Model

(`intake`만 코드 존재. Episode aggregate는 서브도메인이 공유하므로 `domain/episode/domain`에 위치 — [architecture.md](../architecture.md#episode-하위-패키지-관계) 참고)

- `Episode`: `accountId`, `status`(`SYMPTOM_SELECTED`/`INTAKE_COMPLETED`), `symptom`(`@Embedded`), `intake`(`@Embedded`), `bodyParts`(`@ElementCollection`)
- `Symptom`: `angle`(0~360, raw), `radius`(0~1, raw), `primarySymptom`(5종: `DRYNESS_TIGHTNESS`/`ITCHING`/`STINGING`/`REDNESS`/`TROUBLE`), `severity`(4단계: `NORMAL`/`MILD`/`MODERATE`/`SEVERE`). angle/radius로부터 primarySymptom/severity를 서버가 재계산하지 않는다 — FE가 확정해서 보낸 값을 그대로 저장한다.
- `Intake`: `onsetPeriod`(4구간), `recentNewProductName`(자유 텍스트), `notes`(nullable). `bodyParts`는 `WHOLE_FACE`와 다른 부위 동시 선택을 도메인에서 차단한다.

`analysis` 이후 서브도메인은 코드가 없다. 설계는 확정됐지만 엔티티로 아직 옮겨지지 않았다 — 아래 Confirmed Decisions 참고.

## Dependencies

- 서브도메인 간: `intake → analysis → card → routine → checkin` 순차 의존(역방향 지양).
- `analysis`는 명세상 `vanity`(보유 제품 상호작용 태그/기능 태그)와 `tracking`(최근 7일 수면·날씨)을 입력으로 참조할 예정이나, 두 도메인 모두 코드가 없어 실제 연동은 없다. 연동용 read port는 실제 판정 엔진을 구현하는 시점에 설계하기로 했다(소비자가 없는 인터페이스를 미리 만들지 않는다).
- `card`는 결과 카드 화면 하단 진입점을 통해 `story`(같은 증상 태그의 글 개수/목록)에 의존한다. `story`도 미구현 상태라 실제 연동은 없다.
- `card` ↔ `vanity`/`story` 간 실제 조회 방식(동기 호출/read model/캐시 등)과 `analysis`/`card` REST API를 하나로 묶을지 분리할지는 기획 확인 대상이 아니라 backend architecture 결정 사항이다 — 실제 구현 시점에 정한다.

## Confirmed Decisions

**intake (구현 완료)**
- Episode aggregate(`Episode`, `EpisodeStatus` 등)는 `domain.episode.domain`(공유 위치)에 둔다.
- 증상은 5개로 구분한다. 명세의 "표시: 6개 항목" 문구와 "데이터: 5개 증상 구간" 문구가 서로 다른데, 데이터 정의 기준으로 5개로 구현했다. "6개" 문구 쪽은 기획 확인이 필요하다.

**analysis/card — domain 경계**
- `analysis`(2.4)는 원인 후보 순위, 확신 단계, 근거 객체(evidence)를 산출하는 것까지가 책임이다. 그 산출물을 소비해 사용자에게 보여줄 결과 카드 3장을 만드는 것은 `card`(3.1/4.3)의 책임이며, `card`의 선행조건은 "통합 분석 완료"다.

**analysis — 기획에서 확정된 부분**
- 원인 후보는 **4종**으로 확정: 최근 새로 쓰는 제품 / 제품 조합(성분 태그 충돌) / 수면 / 날씨.
- 사진 촬영 단계를 마치거나 건너뛰면 분석을 자동 실행한다. 사진은 필수가 아니며, 건너뛰어도 문진만으로 분석을 진행한다.
- **후보별 근거 강도(강/중/약) 판정 기준**이 확정됐다:
  - **특정 제품**: 사용 시작이 증상 시작보다 앞서고 14일 이내이며, 같은 기간 변경된 다른 제품이 없으면 강. 시점은 맞지만 동시에 변경된 제품이 2개 이상이면 중. 시점 관계가 어긋나면 약.
  - **제품 조합**: 충돌 태그를 가진 제품이 같은 시간대에 배치되면 강. 충돌은 있지만 아침/저녁으로 분리되면 중. 충돌이 없으면 약.
  - **수면 / 날씨**: 관찰 5회 이상 + 일치 비율 70% 이상이면 강. 관찰 3~4회 또는 일치 비율 50~70%면 중. 그 미만이면 약.
- **최종 확신 단계 계산 기준**이 확정됐다:
  - `gap = (1순위 점수 − 2순위 점수) / 1순위 점수` (후보가 하나면 `gap = 1`)
  - **보류**: 후보 0개 / `gap < 0.2` / 1순위 근거 강도가 약
  - **높음**: 1순위 근거 강도가 강 **그리고** `gap >= 0.4`
  - **보통**: 그 외 나머지
- 확신 단계는 화면에 라벨(높음/보통/낮음)이나 퍼센트로 노출하지 않고, 근거를 문장으로 변환해 보여준다. 확신 단계가 "보통"이면 근거 문장에 "기록이 더 모이면 확실해진다"는 안내를 덧붙인다.
- **근거 객체(evidence)는 종류(type)를 갖는 구조화된 데이터**로 확정됐다. Backend가 이 구조화된 evidence object를 산출해 넘기는 것 자체는 (문장 생성 주체와 별개로) 확정된 요구사항이다.
  - 빈도형: 관찰 횟수, 전체 횟수
  - 시점형: 제품 사용 시작일, 증상 시작일
  - 다만 근거 문장을 Backend가 완성 문장으로 내려줄지, Frontend가 구조화된 evidence로 문장을 조립할지는 Backend/API Contract 결정 사항으로 남겨둔다(기획 확인 대상 아님).

**card — 확정된 화면 요구사항**
- 카드 하단에 근거 출처와 근거가 된 기록 일수(예: "N일치 기록")를 표시한다.
- 세 번째 카드(병원 권유)는 원인 후보/확신 단계와 무관하게 **항상 표시**한다.
- 판단 보류 상태("아직 판단하기 이릅니다")일 때는 첫 번째 카드(중단 권유)를 보류 안내로 대체하고, 병원 카드는 그대로 유지한다. 보류여도 보류 사유·오늘 해볼 일반 조언·병원 방문 기준 세 가지는 항상 내려준다.
- "진단이 아닙니다" 고지를 항상 노출하고, 점수나 등급은 표시하지 않는다.

**card ↔ story 연동 — 확정된 부분**
- 결과 카드 화면과 3일차 판정 화면 하단에, 현재 에피소드 증상과 같은 태그의 story 글 개수를 보여주는 진입점 버튼을 노출한다(예: "같은 가려움을 겪은 분들의 이야기 12개"). 카드 3장 영역과는 시각적으로 분리한다.
- 해당 증상 글이 기준 개수 미만이면 버튼 자체를 노출하지 않는다(정확한 기준 개수는 story 도메인 쪽 별도 pending).
- 판정이 나빠짐으로 나와 즉시 중단 안내가 뜨는 화면에서는 이 진입점을 노출하지 않는다.

**analysis — 설계는 확정했지만 위 기획 결정과는 별개로, 코드 구조에 대해 정한 것**
- `EpisodeAnalysisResult`는 `Episode`와 별도 엔티티로 두고, `episodeId` 값 참조를 쓴다(FK나 `@OneToOne` 아님).
- `episodeId`에 unique 제약을 둬서 Episode당 결과가 하나만 존재하게 한다.
- 원인 후보 순서는 `@ElementCollection` + `@OrderColumn`으로 DB 재조회 후에도 보존한다 — 3일차 "다음 원인 후보" 기능의 전제조건.
- "확신 수준"과 "기준 통과 여부(sufficientEvidence)"는 서로 다른 정보라 별개 필드로 분리해서 저장한다.
- 분석 실패(엔진 예외)와 확신 수준 미달("판단하기 이릅니다")은 다른 케이스로 처리한다: 확신 수준 미달은 정상 완료이므로 `ANALYZED` 상태로 전이하고, 진짜 실패는 Episode 상태를 바꾸지 않아 재시도가 그냥 같은 API 재호출이 되게 한다.
- 이미 `ANALYZED`인 Episode에 재요청하면 엔진을 다시 돌리지 않고 기존 결과를 그대로 반환한다(idempotent) — 네트워크 재시도 대응.
- `CauseAnalysisEngine` 인터페이스(입력 Episode → 출력 결과 계약)만 정의하고, 실제 판정 로직 구현체는 만들지 않는다. Controller/Service도 구현체가 없는 상태에서는 만들지 않는다 — 부팅 리스크와, 항상 에러만 나는 API를 배포하는 걸 피하기 위해서다.

## Pending Decisions

- 원인 후보별 실제 score 계산 방식(근거 강도 강/중/약 판정 이후, 최종 점수로 어떻게 환산하는지)
- 원인 후보 자체를 생성하지 않는 조건(후보 0개가 되는 정확한 기준)
- 제품 조합(성분 태그 충돌) 원인 후보의 evidence를 어떤 구조로 표현할지 — 빈도형/시점형 두 타입으로 충분한지 미확인
- 카드에 표시되는 "보습", "세라마이드" 등 태그가 `vanity`의 `InteractionTag`(5종)와 별도 체계(`functionTags`/`keyIngredients`)인지, 그렇다면 어떻게 조합해 내려줄지
- Figma 결과 화면의 "좋아졌다" 버튼이 실제로 어떤 행동을 트리거하는지 — 3일차 판정의 "좋아짐"과 동일한 행동인지, 결과 직후 별도 피드백인지. 현재 기능명세(결과 카드 3장)에는 이 버튼에 대응하는 요구사항이 없다.
- 사진 분석 방식(현재 범위 전체에서 사진 자체가 제외 상태)

## Source of Truth

- intake API 계약: Swagger
- analysis/card 설계: 이 문서(위 Confirmed Decisions) — 아직 코드화되지 않음
- 비즈니스 요구사항: 기능명세서 2~4장
