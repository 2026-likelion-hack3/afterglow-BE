-- 2.4 통합 분석 완료 상태(ANALYZED) 추가로 episode.status 허용값을 넓힌다. V1의 CHECK 제약은 이미
-- 적용됐으므로 직접 수정할 수 없어 드롭 후 재생성한다.

ALTER TABLE episode
    DROP CONSTRAINT episode_status_check;

ALTER TABLE episode
    ADD CONSTRAINT episode_status_check
        CHECK (status IN ('SYMPTOM_SELECTED', 'INTAKE_COMPLETED', 'ANALYZED'));
