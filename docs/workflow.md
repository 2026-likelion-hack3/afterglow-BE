# Workflow

## 브랜치 전략

```
feature/fix/refactor/chore
        ↓ PR (base: dev)
       dev
        ↓ PR (base: main, 릴리즈 시점)
       main
        ↓
   production
```

- `dev`가 integration branch이자 GitHub default branch다. 평소 작업은 전부 `dev`에서 분기하고 `dev`를 base로 PR을 연다.
- `main`은 release/production branch다. `dev`가 배포 가능한 상태가 되면 `dev` → `main` PR로 승격한다.
- 브랜치명/커밋 메시지/이슈·PR 템플릿 등 세부 규칙은 [CONTRIBUTING.md](../CONTRIBUTING.md)가 정본이다 — 이 문서에서 중복 기술하지 않는다.

## 기획/설계가 미확정일 때

- 기획에서 확정되지 않은 비즈니스 규칙(점수, threshold, taxonomy 등)을 개발자가 임의로 만들지 않는다.
- 구현이 막히면 관련 GitHub Issue에 Blocked/Pending 사유를 기록한다.
- 막힌 부분을 무작정 기다리지 말고, 그 작업 안에서 확정 가능한 부분까지만 진행하거나 독립적인 다른 작업으로 전환한다.
