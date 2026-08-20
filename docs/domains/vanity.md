# Vanity

## Responsibility

화장대 — 보유 제품 등록과 조합 주의 경고. 기능명세서 5장.

## Current Status

**엔티티만 구현.** Repository/Service/Controller는 없음.

- `Product`: `accountId`, `name`, `brand`, `type`(자유텍스트), `keyIngredients`, `functionTags`, `openedAt`, `usageTiming`, `interactionTags`(5종), `registrationSource`(`BARCODE`/`PHOTO`/`MANUAL`), `barcode`(nullable), `photoKey`(nullable, 단일 필드)
- `CombinationRule`: `tagA`, `tagB`(nullable), `minCount`, `warningMessage` — 같은 시간대 조합 검사 규칙
- `InteractionTag`(enum, 5개): `RETINOL`, `ACID`, `VITAMIN_C`, `HIGH_CONCENTRATION`, `LOW_IRRITATION`
- `RegistrationSource`(enum, 3개): `BARCODE`, `PHOTO`, `MANUAL`
- `UsageTiming`(enum)

## Planned Features

- 5.1 제품 등록 (아래 Confirmed Decisions에 따라 바코드 제외, 사진은 전면+성분표 2장)
- 5.2 조합 주의 경고: 레티놀×산, 레티놀×비타민씨 동시 시간대 경고 / 고농도 태그 2개 이상 동시 시간대 경고

## Domain Model

Current Status에 적은 엔티티가 현재 코드에 실제로 존재하는 전부다. API/Repository/Service 계층은 없으므로 기술하지 않는다.

## Dependencies

`episode.analysis`가 향후 이 도메인의 제품 상호작용 태그를 참조할 예정(기능명세 2.4)이나, 양쪽 다 실제 연동 코드는 없다.

## Confirmed Decisions

아래는 이 프로젝트에서 **현재도 유효한 확정 결정**이다. 다만 코드와 최신 기능명세서(2026-08-12) 본문에는 아직 반영되지 않았다 — Known Gaps 참고.

- **바코드 등록 경로는 제외한다.**
- 사진 등록은 **제품 전면 + 성분표 사진 2장** 구조로 한다.
- OCR은 **Google Cloud Vision**을 사용한다.
- OCR 결과 정규화를 위해 **성분 동의어 사전**(성분명 → InteractionTag 매핑, 20~30개 큐레이션, 어드민 UI 없이 git으로 관리)을 사용한다.
- 직접 입력 경로는 그대로 유지한다.
- 상호작용 태그 5종(`RETINOL`/`ACID`/`VITAMIN_C`/`HIGH_CONCENTRATION`/`LOW_IRRITATION`)은 확정.
- 조합 경고 규칙(레티놀×산, 레티놀×비타민씨, 고농도 2개 이상)은 기능명세 5.2와 일치하며 확정.

## Known Gaps

현재 코드와 최신 기능명세서 본문이 위 Confirmed Decisions를 아직 따라가지 못하고 있는 부분이다. 이번 문서 정리 작업에서는 Java 코드를 변경하지 않으므로, 실제 구현 시점에 해소해야 할 목록으로 남긴다.

- 기능명세서 5.1/5.1.1(수용 기준 포함)에는 여전히 바코드 등록이 정식 경로로 상세 기술되어 있다 — 기획 쪽에 명세 갱신 필요 여부 확인 필요.
- 코드의 `RegistrationSource`에 `BARCODE` 값이 남아있다.
- `Product.photoKey`는 단일 필드(전면 사진 1장 기준)다 — 전면+성분표 2장 구조로 바꾸려면 필드 추가가 필요하다.
- Google Cloud Vision 연동 코드가 없다.
- 성분표(뒷면) 사진 처리 구조가 없다.
- 성분 동의어 사전은 `RETINOL`/`ACID`/`VITAMIN_C` 3종만 `InteractionTagMatcher`(코드 내 상수, git으로 관리)로 1차 구현됐다 —
  DB 테이블/seed 데이터 형태는 아직 아니고, 키워드도 소수(각 태그당 6~13개)만 curation된 첫 버전이라 확장이
  필요하다. `HIGH_CONCENTRATION`/`LOW_IRRITATION`은 성분명만으로 판단 불가해 의도적으로 매칭 대상에서 제외했다
  (Pending Decisions 참고).

## Pending Decisions

- `Product.type`/`functionTags` 자유텍스트 유지 vs enum 전환
- `CombinationRule`의 실제 경고 문구, 저작/관리 방식
- OCR 실패/저신뢰도 UX(기획/디자인에 이관됨)

## Source of Truth

- 코드 구조: `src/main/java/com/afterglow/domain/vanity`
- **단, 등록 경로(바코드 제외, 사진 2장 구조)는 위 Confirmed Decisions가 최종 방향이며, 코드와 최신 기능명세서 본문은 아직 이를 따라잡지 못한 상태다. "코드에 있다"와 "최종적으로 그렇게 구현하기로 확정됐다"를 같은 뜻으로 읽지 않는다.**
