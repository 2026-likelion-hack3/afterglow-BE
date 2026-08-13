# Notification

## Responsibility

맞춤 알림 설정과 발송, 에피소드 기록 조회 — 기능명세서 8장(현재 `package-info.java` 기준 소관).

## Current Status

**미구현.** `package-info.java`만 존재.

## Planned Features

- **8.1 맞춤 알림 설정**: 3일 루틴 알림과 일상 기록 알림을 각각 켜고 끄고 시각 조정. 누락 기록을 재촉·압박하지 않는다는 원칙. 마케팅/외부 메시지 발송은 제외.
- **8.2 에피소드 기록 조회**: 이전 에피소드의 증상/제품/루틴/일일 체크/판정을 다시 확인. 과거 결과를 현재 상태의 확정 답으로 단정하지 않는다.

## Domain Model

미확정 — 임의로 설계하지 않음.

## Dependencies

8.2는 내용상 `episode`의 과거 기록을 조회하는 기능이라 `episode` 도메인과 강하게 얽힌다. **현재는 `package-info.java` 기준으로 8장 전체가 `notification` 소관으로 되어 있지만, 실제 구현 시점에 8.2가 `episode` 도메인으로 옮겨질 수도 있다 — 이 경계는 아직 최종 확정이 아니다.**

## Confirmed Decisions

없음.

## Pending Decisions

- 채널(push vs email)과 트리거 목록
- 8.2의 최종 domain 소속(notification vs episode)

## Source of Truth

기능명세서 8장. 패키지 소관은 `domain.notification.package-info.java`(현재 상태, 위 Dependencies 참고).
