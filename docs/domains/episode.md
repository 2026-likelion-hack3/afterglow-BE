# Episode

## Responsibility

사건 루프 — 증상 접수부터 3일차 판정까지 하나의 에피소드로 묶이는 흐름. 기능명세서 2~4장.

## Current Status

서브도메인별로 상태가 다르다.

| 서브도메인 | 명세 범위 | 상태 |
|---|---|---|
| `intake` | 2.1~2.3 (사진 촬영 제외) | **구현 완료** |
| `analysis` | 2.4 통합 분석 | 기획 일부 확정 / 핵심 판정 규칙 확인 중 / 코드 구현 보류 |
| `card` | 3.1, 4.3 | 미구현 |
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
- `analysis`는 명세상 `vanity`(보유 제품 상호작용 태그)와 `tracking`(최근 7일 수면·날씨)을 입력으로 참조할 예정이나, 두 도메인 모두 코드가 없어 실제 연동은 없다. 연동용 read port는 실제 판정 엔진을 구현하는 시점에 설계하기로 했다(소비자가 없는 인터페이스를 미리 만들지 않는다).

## Confirmed Decisions

**intake (구현 완료)**
- Episode aggregate(`Episode`, `EpisodeStatus` 등)는 `domain.episode.domain`(공유 위치)에 둔다.
- 증상은 5개로 구분한다. 명세의 "표시: 6개 항목" 문구와 "데이터: 5개 증상 구간" 문구가 서로 다른데, 데이터 정의 기준으로 5개로 구현했다. "6개" 문구 쪽은 기획 확인이 필요하다.

**analysis — 기획에서 확정된 부분**
- 원인 후보는 **4종**으로 확정: 최근 새로 쓰는 제품 / 제품 조합(성분 태그 충돌) / 수면 / 날씨.
- 확신 수준은 **BE가 3단계로 계산**하고, FE는 그 단계에 맞는 문구를 표시한다.

**analysis — 설계는 확정했지만 위 기획 결정과는 별개로, 코드 구조에 대해 정한 것**
- `EpisodeAnalysisResult`는 `Episode`와 별도 엔티티로 두고, `episodeId` 값 참조를 쓴다(FK나 `@OneToOne` 아님).
- `episodeId`에 unique 제약을 둬서 Episode당 결과가 하나만 존재하게 한다.
- 원인 후보 순서는 `@ElementCollection` + `@OrderColumn`으로 DB 재조회 후에도 보존한다 — 3일차 "다음 원인 후보" 기능의 전제조건.
- "확신 수준"과 "기준 통과 여부(sufficientEvidence)"는 서로 다른 정보라 별개 필드로 분리해서 저장한다.
- 분석 실패(엔진 예외)와 확신 수준 미달("판단하기 이릅니다")은 다른 케이스로 처리한다: 확신 수준 미달은 정상 완료이므로 `ANALYZED` 상태로 전이하고, 진짜 실패는 Episode 상태를 바꾸지 않아 재시도가 그냥 같은 API 재호출이 되게 한다.
- 이미 `ANALYZED`인 Episode에 재요청하면 엔진을 다시 돌리지 않고 기존 결과를 그대로 반환한다(idempotent) — 네트워크 재시도 대응.
- `CauseAnalysisEngine` 인터페이스(입력 Episode → 출력 결과 계약)만 정의하고, 실제 판정 로직 구현체는 만들지 않는다. Controller/Service도 구현체가 없는 상태에서는 만들지 않는다 — 부팅 리스크와, 항상 에러만 나는 API를 배포하는 걸 피하기 위해서다.

## Pending Decisions

- 확신 수준 3단계의 실제 판정 기준(어떤 값/조건이 어느 단계에 해당하는지)
- 원인 후보 4종 각각의 성립 조건
- 여러 후보가 나왔을 때의 ranking 기준
- 충분한 후보가 없는 경우("판단하기 이릅니다")의 구체적 처리 기준
- 사진 분석 방식(현재 범위 전체에서 사진 자체가 제외 상태)

## Source of Truth

- intake API 계약: Swagger
- analysis 설계: 이 문서(위 Confirmed Decisions) — 아직 코드화되지 않음
- 비즈니스 요구사항: 기능명세서 2~4장
