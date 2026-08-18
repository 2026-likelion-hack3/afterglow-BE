# Episode

## Responsibility

사건 루프 — 증상 접수부터 3일차 판정까지 하나의 에피소드로 묶이는 흐름. 기능명세서 2~4장.

## Current Status

서브도메인별로 상태가 다르다.

| 서브도메인 | 명세 범위 | 상태 |
|---|---|---|
| `intake` | 2.1~2.3 (사진 촬영 제외) | **구현 완료** |
| `analysis` | 2.4 통합 분석 | 기획 blocker 전부 해소. **판정 엔진(`CauseAnalysisEngine`/`RuleBasedCauseAnalysisEngine`) 구현 완료** — candidate 수집, 근거 강도, ranking/HOLD/confidence까지 순수 함수로 동작. Vanity/Tracking 실제 데이터 연동(Repository, `EpisodeAnalysisService`, Controller/API, DB 저장)은 두 도메인에 데이터 소스가 생긴 뒤 별도로 진행 — 아래 Confirmed Decisions 참고 |
| `card` | 3.1, 4.3 | **`AnalysisResult` → 결과 카드 3장을 조립하는 순수 로직(`ResultCardAssembler`) 구현 완료.** 실제 제품명/이미지(Vanity), 이야기 진입점 개수(Story), Controller/API/DB 저장은 이번 범위 밖 — 아래 Confirmed Decisions 참고 |
| `routine` | 3.2 | 미구현 |
| `checkin` | 4.1~4.2 | **4.1(일일 1탭 체크) "날짜별 상태 기록" 저장/조회 구현 완료. 4.2(3일차 판정)는 순수 판정 규칙(`Day3JudgmentEngine`)만 구현 완료.** 저장된 CheckIn을 실제 Day1/Day2/Day3에 매핑하는 orchestration은 `routine`의 시작일이 있어야 가능해 아직 없음(아래 Confirmed Decisions 참고) — Routine 구현 후 별도 진행 |

## Planned Features

- **card**: 결과 카드 3장(중단/사용/병원 권유), 빈 범주 안내
- **routine**: 3일 루틴 생성
- **checkin**: 일일 1탭 체크, 3일차 판정 분기

세 서브도메인 모두 analysis 결과가 있어야 진행 가능해, analysis 구현 전까지는 착수하지 않는다.

## Domain Model

(`intake`/`analysis`/`checkin`만 코드 존재. Episode aggregate는 서브도메인이 공유하므로 `domain/episode/domain`에 위치 — [architecture.md](../architecture.md#episode-하위-패키지-관계) 참고)

- `Episode`: `accountId`, `status`(`SYMPTOM_SELECTED`/`INTAKE_COMPLETED`), `symptom`(`@Embedded`), `intake`(`@Embedded`), `bodyParts`(`@ElementCollection`)
- `Symptom`: `angle`(0~360, raw), `radius`(0~1, raw), `primarySymptom`(5종: `DRYNESS_TIGHTNESS`/`ITCHING`/`STINGING`/`REDNESS`/`TROUBLE`), `severity`(4단계: `NORMAL`/`MILD`/`MODERATE`/`SEVERE`). angle/radius로부터 primarySymptom/severity를 서버가 재계산하지 않는다 — FE가 확정해서 보낸 값을 그대로 저장한다.
- `Intake`: `onsetPeriod`(4구간), `recentNewProductName`(자유 텍스트), `notes`(nullable). `bodyParts`는 `WHOLE_FACE`와 다른 부위 동시 선택을 도메인에서 차단한다.
- `analysis`는 엔티티가 아니라 순수 판정 엔진이다(DB 저장 없음, `domain.episode.analysis.domain` 패키지):
  - `CauseAnalysisEngine`(interface) / `RuleBasedCauseAnalysisEngine`(구현체) — `AnalysisInput` → `AnalysisResult`.
  - 입력: `AnalysisInput`(analysisDate + `ProductCandidateInput`/`CombinationCandidateInput`/`ObservationCandidateInput`×2(수면/날씨)). **`RecordCoverage`(최초 기록일)는 `ObservationCandidateInput`(수면/날씨)만 갖는다(2026-08-18 정정)** — `ProductCandidateInput`/`CombinationCandidateInput`에는 coverage 필드 자체가 없다. 이유는 아래 "후보 수집(candidate 생성) 조건" 참고.
  - 출력: `AnalysisResult`(정렬된 `CandidateResult` 목록, `CandidateExclusion` 목록, hold 여부, topCandidate, `Confidence`). `topCandidate`/`confidence`는 "최종 선택된 원인"을 뜻하며 `candidates.get(0)`(정렬상 1번째)과는 다르다 — same-type tie로 HOLD면 둘 다 null이다(이 불변식은 `AnalysisResult`의 compact constructor가 강제한다). 게이트에서 제외된 후보는 `candidates`가 아니라 `exclusions`(`CandidateExclusion`: type + `ExclusionReason`(`NO_TARGET`/`INSUFFICIENT_RECORDS`) + 식별자)에 남는다. `CandidateResult`는 `CandidateType`/`EvidenceStrength`/`Evidence`(sealed: `TimingEvidence`/`CombinationEvidence`/`FrequencyEvidence`)/`coverageDays`(`Long`, 2026-08-17 추가)로 구성 — **`coverageDays`는 SLEEP/WEATHER만 실제 값을 갖고, PRODUCT/COMBINATION은 항상 null이다(2026-08-18 정정)**.
- `card`는 엔티티가 아니라 순수 조립 로직이다(DB 저장 없음, `domain.episode.card.domain` 패키지): `ResultCardAssembler.assemble(AnalysisResult)` → `ResultCardResult`(hold 여부, `ResultCardHoldReason`, `Confidence`, 항상 3장인 `ResultCard` 목록). `ResultCard`는 `type`(`ResultCardType`)/`causeType`/`evidence`(기존 Analysis `Evidence` 재사용)/`coverageDays`로 구성 — 실제 문장(제목/이유 한 줄)은 만들지 않는다(아래 Confirmed Decisions 참고).
- `checkin`은 `CheckIn` 엔티티(`domain.episode.checkin.domain`, checkin 서브도메인 고유 데이터라 공유 위치가 아님)로 구현했다: `episodeId`(값 참조, FK 없음), `checkInDate`, `status`(`CheckInStatus`: `IMPROVED`/`SAME`/`WORSE`). `episodeId`+`checkInDate` UNIQUE(DB 제약 포함). `overwrite(status)`로 같은 날짜 재기록 시 값을 덮어쓴다(Confirmed Decisions 참고).
- 4.2 판정은 엔티티/DB 저장 없이 순수 함수로 구현했다: `Day3JudgmentEngine`(`Day3JudgmentInput` → `Day3JudgmentResult`, Repository/Spring 의존 없음) + `Day3JudgmentResult`(`Day3Verdict`: `WITHHELD`/`MAINTAIN`/`EXTEND`/`STOP`). `Day3JudgmentInput`은 `day1Status`/`day2Status`/`day3Status`(각 nullable, 건너뛴 날)를 이미 정리된 값으로 받는다 — **저장된 `CheckIn`(#37, 날짜만 가짐)을 실제 Day1/Day2/Day3에 매핑하는 로직은 이번 범위에 없다.** 그 매핑에는 Routine의 시작일이 필요한데 `routine`이 아직 구현되지 않아 존재하지 않는다(아래 참고). 매핑 orchestration이 없으니 이 엔진을 호출하는 application/Controller 계층도 아직 없다.

`routine` 서브도메인은 코드가 없다. 설계는 확정됐지만 아직 옮겨지지 않았다 — 아래 Confirmed Decisions 참고.

## Dependencies

- 서브도메인 간: `intake → analysis → card → routine → checkin` 순차 의존(역방향 지양).
- `analysis`의 판정 엔진 자체는 `vanity`/`tracking`을 코드로 의존하지 않는다(정규화된 입력 모델만 안다). 다만 실제 운영에서 그 입력을 채우려면 두 도메인에서 아래 데이터가 필요하다 — 두 도메인 모두 아직 코드가 없어 실제 연동은 없다:
  - **`vanity`에서 필요**: 계정의 제품별 사용 시작일(`ProductCandidateInput.usageStartDate`), 같은 기간 내 사용 시작한 제품 수(`changedProductCountInWindow`), 충돌 태그를 가진 제품 쌍과 그 배치(같은 time slot/AM·PM 분리, `CombinationCandidateInput`) — 즉 최소 "제품별 사용 시작일 조회"와 "보유 제품 중 충돌 조합 판별" 기능이 필요하다. **`usageStartDate`의 실제 source는 Pending(2026-08-18)** — Vanity `Product`에는 "개봉일"(`openedAt`)만 있고, Manyfast에 "개봉일 = 사용 시작일"이라는 대응이 정의돼 있지 않다. `openedAt`을 자동으로 매핑하지 않는다.
  - **`tracking`에서 필요**: 최근 수면/날씨 관측 횟수와 그중 증상과 일치한 횟수(`ObservationCandidateInput`), 그리고 각 후보의 최초 기록일(coverage) — 즉 최소 "계정별 수면/날씨 관측 집계" 기능이 필요하다. **수면 raw-level → matched-observation count 산출 규칙은 Pending(2026-08-18)** — Tracking은 `SleepLevel`(WELL/NORMAL/POOR 3단계)만 저장하고, 이를 증상과 "일치"로 볼지 정하는 규칙이 Manyfast에 없다. **날씨(온도/습도/자외선) matching threshold도 Pending(2026-08-18)** — Tracking은 raw 수치만 저장하고, 어떤 값이 증상과 "일치"인지 정하는 threshold가 Manyfast에 없다.
  - 실제 read port(Repository/Service 호출 방식)는 두 도메인의 데이터가 준비된 뒤, `EpisodeAnalysisService`를 만드는 시점에 설계한다(소비자가 없는 인터페이스를 미리 만들지 않는다).
- `card`는 결과 카드 화면 하단 진입점을 통해 `story`(같은 증상 태그의 글 개수/목록)에 의존한다. `story`도 미구현 상태라 실제 연동은 없다.
- `card` ↔ `vanity`/`story` 간 실제 조회 방식(동기 호출/read model/캐시 등)과 `analysis`/`card` REST API를 하나로 묶을지 분리할지는 기획 확인 대상이 아니라 backend architecture 결정 사항이다 — 실제 구현 시점에 정한다.

## Confirmed Decisions

**intake (구현 완료)**
- Episode aggregate(`Episode`, `EpisodeStatus` 등)는 `domain.episode.domain`(공유 위치)에 둔다.
- 증상은 5개로 구분한다. 명세의 "표시: 6개 항목" 문구와 "데이터: 5개 증상 구간" 문구가 서로 다른데, 데이터 정의 기준으로 5개로 구현했다. "6개" 문구 쪽은 기획 확인이 필요하다.
- **Account 삭제 lifecycle 정합성(2026-08-17, Issue #32)**: `EpisodeAccountDeletedListener`(`domain.episode.application`, Onboarding과 동일하게 `@TransactionalEventListener(BEFORE_COMMIT)`)가 `AccountDeletedEvent`를 구독해 그 계정 소유 Episode와, 그 Episode들의 CheckIn(`episodeId` 값 참조라 FK cascade가 없어 Episode보다 먼저 지운다)을 같은 트랜잭션에서 정리한다. `EpisodeService.createEpisode`는 `submitIntake`와 달리 아직 episodeId가 없어 ownership lookup으로 보호할 수 없으므로, `AccountRepository`로 계정 존재를 직접 확인한 뒤 생성한다(삭제된 계정의 유효한 JWT로 새 Episode 생성 차단). `submitIntake`/CheckIn API는 기존 `findByIdAndAccountId` ownership lookup만으로 이미 충분해 — cleanup 이후 해당 Episode 자체가 없으므로 — 추가로 손대지 않았다. Analysis/Card/Day3 판정은 DB에 저장하지 않으므로 cleanup 대상이 아니다.

**analysis/card — domain 경계**
- `analysis`(2.4)는 원인 후보 순위, 확신 단계, 근거 객체(evidence)를 산출하는 것까지가 책임이다. 그 산출물을 소비해 사용자에게 보여줄 결과 카드 3장을 만드는 것은 `card`(3.1/4.3)의 책임이며, `card`의 선행조건은 "통합 분석 완료"다.

**analysis — 기획에서 확정된 부분**
- 원인 후보는 **4종**으로 확정: 최근 새로 쓰는 제품 / 제품 조합(성분 태그 충돌) / 수면 / 날씨.
- 사진 촬영 단계를 마치거나 건너뛰면 분석을 자동 실행한다. 사진은 필수가 아니며, 건너뛰어도 문진만으로 분석을 진행한다.
- **후보별 근거 강도(강/중/약) 판정 기준**이 확정됐다:
  - **특정 제품**: 사용 시작이 증상 시작보다 앞서고 14일 이내이며, 같은 기간 변경된 다른 제품이 없으면 강. 시점은 맞지만 동시에 변경된 제품이 2개 이상이면 중. 시점 관계가 어긋나면 약.
  - **제품 조합**: 충돌 태그를 가진 제품이 같은 시간대에 배치되면 강. 충돌은 있지만 아침/저녁으로 분리되면 중. 충돌이 없으면 약.
  - **수면 / 날씨**: 관찰 5회 이상 + 일치 비율 70% 이상이면 강. 관찰 3~4회 또는 일치 비율 50~70%면 중. 그 미만이면 약. **70% 경계값 확정(2026-08-16)**: "강" 조건(관찰≥5 그리고 일치율≥70%)을 먼저 평가하고, 이를 만족하지 못하면 "중" 조건(관찰 3~4 또는 일치율 50~70%, 양 끝 포함)을 본다 — 즉 정확히 70%는 관찰이 5회 이상일 때만 강이고, 5회 미만이면 중으로 fallback된다. 정수 관측 횟수에서는 "관찰 5회 미만 + 일치율 정확히 70%"는 산술적으로 나올 수 없다(70%를 정수 분수로 표현하려면 분모가 최소 10 이상이어야 한다) — 이 조합 자체가 불가능하다는 점도 확인했다.
- ~~후보별 근거 강도를 점수로 환산하는 기준(강=3/중=2/약=1)이 확정됐다~~ — **2026-08-19 폐기**: 아래 "순위/보류/확신 단계" 참고. 점수 환산과 그에 기반한 격차(gap) 계산은 더 이상 쓰지 않는다.
- **후보 수집(candidate 생성) 조건**이 확정됐다(2026-08-13 추가 답변):
  - 대상 없음(예: 최근 새로 쓰는 제품 없음, 충돌 조합 없음) → 후보 목록에서 제외, 사유 `대상 없음`. 이 조건은 **4종 후보 공통**이다.
  - **기록 7일 미만 → 후보 목록에서 제외, 사유 `기록 부족`. 이 조건은 수면/날씨 2종에만 적용된다 — 제품/제품 조합에는 적용되지 않는다(2026-08-18 정정, 아래 "왜 바뀌었나" 참고).**
  - 대상 있음(그리고 수면/날씨는 7일 게이트까지 통과) → 후보 목록에 추가하고 위 근거 강도(강/중/약) 계산 진행. 제품/제품 조합은 대상만 있으면 coverage 검사 없이 바로 강도 계산으로 넘어간다.
  - **"기록 7일" 게이트 정의 확정(2026-08-16, 적용 대상은 2026-08-18 정정)**: record 건수가 아니라 **calendar-day coverage**다 — 해당 후보 데이터의 최초 기록일부터 분석 기준일(`episode.createdAt`의 날짜)까지의 날짜 수(양 끝 포함)가 7 이상이면 통과한다(예: 8/10~8/16 = 7일 → 통과). 관찰 횟수(수면/날씨의 3~5회 이상)나 14일 timing window(특정 제품)와는 별개의 규칙이며, 하나로 합치지 않는다 — 관찰 3~4회여도 최초 기록일이 7 calendar days 이상 전이면 이 게이트는 통과할 수 있다(그 다음 근거 강도 판정에서 "관찰 3~4회 → 중"으로 별도 평가된다).
  - **왜 바뀌었나(2026-08-18)**: 이전 문서는 이 게이트를 "4종 후보 각각에 공통으로 적용"이라고 기술했었다. Manyfast updateData 원문 재확인 결과 7일 미만 제외 대상이 수면과 날씨로 명시되어 있었음 — 통합 분석(F-ZSPZHH) `updateData`의 "[후보 수집]" 규칙 원문: "검증할 대상이 없으면 목록에서 제외하고 사유를 대상없음으로 남긴다. 일일 기록이 **7일 미만이면 수면과 날씨를** 제외하고 사유를 기록부족으로 남긴다. 대상은 있는데 근거가 부실한 경우는 제외하지 않고 강도 약으로 목록에 남긴다." 이 원문이 명시적으로 "수면과 날씨"라고만 못박고 있어, 제품/제품 조합까지 이 게이트를 적용하던 기존 구현·문서 서술은 잘못된 해석이었다. 코드(`RuleBasedCauseAnalysisEngine`)는 이 정정을 반영해 제품/제품 조합에서 coverage 검사와 `INSUFFICIENT_RECORDS` exclusion 로직을 제거했다.
- **순위/보류(withhold)/확신 단계 계산 기준이 정정됐다(2026-08-19, Manyfast 통합 분석 F-ZSPZHH 최신 확정)** — 이전에는 강/중/약을 점수(3/2/1)로 환산해 1·2순위 점수 격차(gap)로 보류·확신을 판정했으나, 최신 원문이 이 방식을 명시적으로 폐기했다: "강·중·약을 점수로 바꿔 나누는 격차 계산은 쓰지 않는다. 값이 세 가지뿐이라 임계값이 의미를 잃고, 같은 종류 안의 동점을 가를 수 없기 때문이다." 대신 순수 정렬 규칙으로 순위를 정한다(gap 계산이 아니라는 점을 원문이 직접 못박음):
  - **1차**: 근거 강도 순(강 > 중 > 약).
  - **2차**: 강도가 같으면 type priority(되돌리기 쉬운 순서 — 특정 제품 > 제품 조합 > 수면 > 날씨). "종류가 다른 후보끼리는 점수 차이로 보류하지 않는다"(원문) — 강도만 같으면 격차 계산 없이 이 순서로 즉시 갈린다.
  - **3차**: type과 강도까지 같으면 같은 종류 안의 tie-break — **특정 제품은 사용 시작일이 최근인 후보 우선**, **제품 조합은 충돌이 센 후보 우선**(같은 시간대 > 아침/저녁 분리 > 충돌 없음). 수면/날씨는 후보가 타입당 최대 하나뿐이라 같은 타입끼리 동점 자체가 나올 수 없다 — 원문에도 별도 tie-break가 없다.
  - 위 세 단계를 모두 적용해도 1위를 가릴 수 없으면(예: 특정 제품 두 개가 강도·사용 시작일까지 완전히 같음) **보류**다.
  - **보류 조건 정리**: 후보 0개 / 1순위 근거 강도가 약 / 정렬 규칙을 끝까지 적용해도 1위를 가릴 수 없음.
  - **확신 단계**(보류가 아닐 때만 적용): 1순위가 강이고 2순위가 약이거나 없으면 높음. 그 외는 보통.
  - `EvidenceStrength`의 점수(3/2/1) 환산과 `CandidateResult.score()`는 더 이상 쓰이지 않아 코드에서 제거했다 — 순위 비교는 강도 enum의 선언 순서(강 → 중 → 약)만으로 충분하다.
- 확신 단계는 화면에 라벨(높음/보통/낮음)이나 퍼센트로 노출하지 않고, 근거를 문장으로 변환해 보여준다. 확신 단계가 "보통"이면 근거 문장에 "기록이 더 모이면 확실해진다"는 안내를 덧붙인다.
- **근거 객체(evidence)는 종류(type)를 갖는 구조화된 데이터**로 확정됐다. Backend가 이 구조화된 evidence object를 산출해 넘기는 것 자체는 (문장 생성 주체와 별개로) 확정된 요구사항이다. **후보 유형별 evidence 종류 매핑이 확정됐다(2026-08-13 추가 답변)**:
  - 특정 제품 → 시점형(timing) evidence: 제품 사용 시작일, 증상 시작일
  - 수면 / 날씨 → 빈도형(frequency) evidence: 관찰 횟수, 전체 횟수
  - 제품 조합 → combination evidence(`CombinationEvidence`: 충돌 태그 쌍 + 배치 방식) 구조 확정(2026-08-16, 아래 코드 구조 결정 참고).
  - 다만 근거 문장을 Backend가 완성 문장으로 내려줄지, Frontend가 구조화된 evidence로 문장을 조립할지는 Backend/API Contract 결정 사항으로 남겨둔다(기획 확인 대상 아님).

**checkin — 4.1(일일 1탭 체크), 기획에서 확정된 부분(Manyfast F-SQUDJA)**
- 응답값은 3단계(`IMPROVED`/`SAME`/`WORSE`, "처음 시작했을 때와 비교해" 좋아졌다/비슷하다/나빠졌다)로 확정.
- **같은 날짜에 여러 번 기록하면 마지막 응답으로 덮어쓴다**(F-SQUDJA exceptions: "하루에 여러 번 누르면 마지막 응답으로 덮어쓴다") — 중복 입력을 거절하지 않고 upsert한다. 코드에서는 `CheckInService.record`가 find-or-create 후 `CheckIn.overwrite()`로 구현했다.
- 건너뛴 날은 별도 처리 없이 그냥 빈 값으로 둔다(독촉 안내 등 없음) — 굳이 "결석" 개념을 저장하지 않는다.
- 원래 기획상 전제조건은 "3일 루틴을 시작한 상태"이지만, `routine`이 아직 구현 전이라 이번 범위에서는 그 전제조건을 걸지 않고 **Episode 소유 여부만 검증**한다(`EpisodeRepository.findByIdAndAccountId`). Routine이 실제로 구현되면 이 전제조건을 다시 검토한다.

**checkin — 4.2(3일차 판정과 분기), 기획에서 확정된 부분(Manyfast F-TWLPPZ)**
- 최종 판정은 유지/연장/중단 3가지 + 기록 부족 시 보류, 총 4가지로 확정: `Day3Verdict.MAINTAIN`/`EXTEND`/`STOP`/`WITHHELD`.
- **판정은 3일차 응답을 기준으로 한다.** 1·2일차 응답은 참고용이며 판정 자체를 바꾸지 않는다 — 단, "1일차나 2일차에 나빠졌다가 나오면 3일을 기다리지 않고 즉시 중단 안내로 분기한다"는 별도 확정 예외가 있어, 사실상 **Day1/2/3 어디서든 나빠졌다(WORSE)가 한 번이라도 있으면 STOP**이 된다(두 규칙의 합집합으로 구현).
- 나빠졌다가 없고 응답이 2일 이하면 판정하지 않고 보류(`WITHHELD`, "판단하기 이릅니다").
- 나빠졌다가 없고 3일 모두 응답이면 Day3 응답 기준: 좋아졌다 → `MAINTAIN`, 비슷하다 → `EXTEND`.
- 비슷함(`EXTEND`)의 실제 후속 동작은 "다음 원인 후보로 이동" 또는 "3일 연장 제안" 중 기획에 명시된 대로 상황에 따라 갈리는데, 그 분기 기준(어떤 조건에서 어느 쪽을 택하는지)은 기획에 명시돼 있지 않다 — 이 판단은 Card/Analysis 영역의 몫으로 보고, 이번 엔진은 "연장이 필요한 애매한 상태"라는 뜻의 `EXTEND` 판정까지만 낸다.
- Day3 판정 결과를 저장해야 한다는 기획 요구가 없어 DB에 저장하지 않는다.
- Day3 판정을 조회/실행하는 REST API 계약이 기획에 명시돼 있지 않아 Controller/API는 만들지 않았다.

**checkin — 4.2 구현 축소 사유(2026-08-17 정정)**
- "Day N"이 무엇인지는 F-SQUDJA의 "비교 기준을 어제가 아니라 **시작 시점**으로 고정"이라는 문구가 근거다 — Day1/2/3은 Routine 시작일에 고정된 날짜 슬롯이지, "응답이 들어온 순서"가 아니다. "건너뛴 날은 빈 값으로 둔다"(F-SQUDJA exceptions)도 그 슬롯이 비어있다는 뜻이지, 응답 개수만 세면 된다는 뜻이 아니다.
- 처음 구현에서는 저장된 `CheckIn`을 `checkInDate` 오름차순으로 정렬해 "N번째 응답 = Day N"으로 간주했는데, 이 대응이 Manyfast에 명시적으로 확정돼 있지 않다는 점과 건너뛴 날이 있으면 실제 Day3가 아닌 응답을 Day3로 오판할 수 있다는 문제가 발견되어(예: 8/1 응답, 8/2 건너뜀, 8/3 응답 후 8/7에 응답이 하나 더 오면 8/7을 "3번째 응답"으로 보고 Day3로 오판) 되돌렸다.
- 그래서 `Day3JudgmentEngine`은 이미 Day1/Day2/Day3로 정리된 `Day3JudgmentInput`만 받고, "어떤 CheckIn이 몇 일차인지" 판단하는 orchestration(Service/Controller)은 만들지 않았다 — 그 매핑에 필요한 Routine 시작일이 아직 없기 때문이다. `routine`이 구현되면 그 매핑과 이 엔진을 호출하는 계층을 별도로 추가한다.
- 같은 이유로 "3일 초과 기록은 처음 3건만 본다"는 규칙도 기획에 명시된 바 없어 확정하지 않았다 — 3일 연장 이후의 재판정 방식은 미확정 기능으로 남는다.

**card — 확정된 화면 요구사항(Manyfast F-HGUJDZ, 통합 분석 updateData)**
- 카드는 항상 3장: (1) 원인 후보 관련 카드 (2) 오늘 사용할 것 (3) 병원 방문 기준.
- 세 번째 카드(병원 권유)는 원인 후보/확신 단계와 무관하게 **항상 표시**한다.
- 판단 보류 상태("아직 판단하기 이릅니다")일 때는 첫 번째 카드(중단 권유)를 보류 안내로 대체하고, 병원 카드는 그대로 유지한다. 보류여도 보류 사유·오늘 해볼 일반 조언·병원 방문 기준 세 가지는 항상 내려준다(통합 분석 updateData exceptions).
- **원인 후보가 WEATHER면 멈출 대상이 없으므로 첫 번째 카드가 "오늘 중단할 것"(`DISCONTINUE`) 대신 "오늘 더 해줄 것"(`DO_MORE_TODAY`)으로 바뀐다** — 통합 분석 updateData exceptions: "날씨가 1순위면 멈출 대상이 없으므로 중단할 것 카드 대신 오늘 더 해줄 것 카드로 바꾼다."
- **"N일치 기록" = `RecordCoverage.coverageDays`(2026-08-17 확정, 적용 대상은 2026-08-18 정정)** — Analysis의 7-day gate에 쓰는 그 calendar coverage 값과 동일하다. `FrequencyEvidence.observationCount`/`matchedObservationCount`(확신 단계 문장에 쓰는 "N번 중 M번" 관찰 횟수)와는 **서로 다른 숫자**이며 섞지 않는다. **이 값은 원인 후보가 SLEEP/WEATHER일 때만 존재한다(2026-08-18 정정)** — 7-day coverage 게이트 자체가 수면/날씨 전용이라 PRODUCT/COMBINATION은 coverage 시작점이라는 개념 자체가 없다. `ResultCard.coverageDays`는 그래서 causeType이 PRODUCT/COMBINATION이면 항상 null이고, 카드는 이때 "N일치 기록에 근거"라는 임의 숫자를 만들어 채우지 않는다. **PRODUCT/COMBINATION Result Card에서 근거 기간을 무엇으로 보여줄지는 Pending(2026-08-18)** — 기획에 정의돼 있지 않다.
- **보류 사유는 `CandidateExclusion`으로 구분한다(2026-08-17 확정, 구현 범위)**: 후보 목록이 비어 있고(모든 타입 제외) 그중 하나라도 `INSUFFICIENT_RECORDS`면 "기록이 더 모이면" 류의 안내, 전부 `NO_TARGET`이면 그런 약속을 하지 않는다(통합 분석 updateData exceptions). 후보가 있었지만 근거 부족/정렬 규칙으로도 못 가른 동점으로 보류된 경우는 별도 사유(`INCONCLUSIVE_EVIDENCE`)로 구분한다 — 정확한 문구는 기획에 없어 만들지 않았다.
- 카드 자체에는 별도 태그 체계가 없다(2026-08-17 확정, 아래 Pending Decisions 정정 참고) — dataSpec은 "제목/대상 제품 또는 기준/이유 한 줄/근거 출처"뿐이다. "근거 출처"는 기존 `CandidateType`/`Evidence`로 표현되며 새 타입 체계가 필요 없다.
- 두 번째 카드(오늘 사용할 것)는 보유 제품 "전체" 목록에서 구성해야 하는데 이건 Vanity 데이터가 있어야 가능하다 — 이번 범위에서는 카드 자리(`CONTINUE_USE` 타입)만 만들고 내용은 비워둔다.
- "진단이 아닙니다" 고지를 항상 노출하고, 점수나 등급은 표시하지 않는다(UI 고정 문구·규칙이라 backend 데이터 모델에 넣지 않았다).
- 제목/이유 한 줄 같은 실제 문장은 이번에 만들지 않는다 — 제품명(Vanity), 증상명(현재 `ResultCardAssembler` 입력이 `AnalysisResult`뿐이라 Episode/Symptom 미포함) 등 문장 조립에 필요한 데이터가 부족하다. 대신 구조화된 근거(`causeType`/`evidence`/`coverageDays`)만 반환한다.

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
- **(2026-08-16 변경)** 이전에는 "`CauseAnalysisEngine` 인터페이스만 정의하고 실제 구현체는 만들지 않는다"였으나, 판정 규칙 자체가 이미 전부 확정되어 데이터 연동과 독립적으로 구현·검증할 수 있다는 판단에 따라 결정을 바꿨다. 새 기준:
  - `CauseAnalysisEngine`의 **실제 구현체(`RuleBasedCauseAnalysisEngine`)를 지금 구현한다.** 입력(`AnalysisInput`)만으로 결정적(pure/deterministic)으로 동작하며, Repository·Spring Bean·외부 호출·`LocalDateTime.now()`에 의존하지 않는다 — 분석 기준일은 항상 입력으로 받는다.
  - 엔진은 Vanity/Tracking의 실제 엔티티나 concrete 타입을 참조하지 않는다. 대신 엔진이 판단하기에 필요한 최소한으로 정규화된 입력 모델(`ProductCandidateInput`/`CombinationCandidateInput`/`ObservationCandidateInput`)만 안다.
  - **Controller/API, `EpisodeAnalysisService`(orchestration), Vanity/Tracking 실제 데이터 조회, 분석 결과 DB 저장(V3 migration)은 여전히 만들지 않는다.** Vanity에는 아직 Repository가, Tracking에는 아직 엔티티가 전혀 없어(둘 다 팀원 담당 영역), 지금 이 계층까지 만들면 항상 "대상 없음"만 반환하는 API를 배포하게 된다 — 이 부분은 이전 결정의 우려(부팅 리스크·항상 에러/빈 응답만 나는 API)가 여전히 유효하므로 그대로 유지한다. 실제 연동은 두 도메인의 데이터가 준비된 뒤 별도 Issue로 진행한다.

## Pending Decisions

**기획 확인 필요 (아직 열려 있는 질문)**

- **(2026-08-17 정정)** ~~카드에 표시되는 "보습", "세라마이드" 등 태그가 vanity의 InteractionTag(5종)와 별도 체계인지~~ — 재확인 결과 이건 Result Card(F-HGUJDZ)가 아니라 **"빈 범주 안내"라는 별개 기능**의 내용이었다(범주: 진정/장벽 강화/보습/자외선 차단/각질 관리 — Vanity `InteractionTag`와는 확실히 다른 체계). Result Card 자체의 dataSpec에는 태그 필드가 없다. "빈 범주 안내"는 이번 범위 밖이며, 그 기능을 다룰 때 다시 확인한다.
- Figma 결과 화면의 "좋아졌다" 버튼이 실제로 어떤 행동을 트리거하는지 — **여전히 미확정.** "좋아졌다"라는 단어는 Manyfast 전체에서 Daily Check-in(F-SQUDJA)과 Day3 판정(F-TWLPPZ) 설명에만 등장하고, Result Card(F-HGUJDZ)의 action/outcome/rules 어디에도 카드 화면 자체에 이런 버튼이 있다는 문구가 없다(카드 안에 둘 수 있는 보조 버튼은 "이야기로 이동" 버튼뿐이라고 명시됨). 이번 Result Card 구현에서도 관련 API/Check-in 연결/navigation을 구현하지 않았다.
- **(2026-08-18 신규) `usageStartDate` ↔ Vanity `openedAt` 대응 여부** — Manyfast에 "개봉일 = 사용 시작일"이라는 명시적 대응이 없다. `ProductCandidateInput.usageStartDate` 개념은 코드에 유지하되, 실제 어떤 Vanity 데이터로 채울지는 이 질문이 풀려야 정한다.
- **(2026-08-18 신규) PRODUCT/COMBINATION Result Card의 근거 기간 표시 기준** — 7-day coverage 게이트가 수면/날씨 전용으로 정정되면서, 제품/제품 조합 카드에서 "N일치 기록" 대신 무엇을 근거 기간으로 보여줄지(또는 아예 보여주지 않을지)가 기획에 정의돼 있지 않다.
- **(2026-08-18 신규) 수면 raw-level(SleepLevel: WELL/NORMAL/POOR) → matched-observation 판정 규칙** — `ObservationCandidateInput.matchedObservationCount`를 실제 Tracking 데이터로 채우려면 어떤 SleepLevel(들)을 "증상과 일치"로 볼지 규칙이 필요한데 Manyfast에 없다. Manyfast 예시 문구("잠이 5시간 아래였던 날")는 시간 단위인데 실제 저장 데이터는 3단계 coarse enum이라 예시와 데이터 모델 간 불일치도 있다.
- **(2026-08-18 신규) 날씨(온도/습도/자외선) matching threshold** — Tracking은 raw 수치만 저장 명세돼 있고, 어떤 값을 증상과 "일치"로 볼지 threshold가 Manyfast에 없다(수면과 동일한 성격의 공백).

**Implementation Decisions Pending (기획 blocker 아님 — backend 구현 시점에 정할 것)**

- **사진(선택 촬영) 사용 여부**: 2026-08-16 결정 — 이번 판정 엔진 구현에서는 사진을 candidate 강도 계산에 포함하지 않는다(엔진 입력에 사진 관련 필드 자체가 없다). "3일차(Day 3) 비교 전용으로 쓴다"는 방향은 유지하되, 그 기능은 아직 구현하지 않는다. Episode에 이미 있는 사진 데이터는 그대로 보존만 하고 이번 범위에서 새로 다루지 않는다.

**Backend 구현 완료(2026-08-16), 아래 참고**

- ~~70% 경계값~~ / ~~"기록 7일 미만" 제외 조건 정의~~ / ~~제품 조합 evidence DTO 구조~~ → 위 Confirmed Decisions에 반영 완료.

## Source of Truth

- intake API 계약: Swagger
- analysis/card 설계: 이 문서(위 Confirmed Decisions) — 아직 코드화되지 않음
- 비즈니스 요구사항: 기능명세서 2~4장
