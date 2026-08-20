-- RC1 데모용 Story seed (2026-08-20)
--
-- 목적: Story 탭이 "시드 글 읽기 전용"으로 나가는 RC1 데모에서 GET /api/stories(목록/태그 필터)와
-- GET /api/stories/{id}(상세)가 빈 화면이 아니라 실제 데이터로 보이게 한다.
--
-- 전제:
-- - V13__story_like.sql까지 적용된 schema를 대상으로 한다(Flyway 마이그레이션을 먼저 실행해 둘 것).
-- - 이 스크립트가 만드는 API endpoint는 없다 — 수동으로 한 번 실행하는 순수 SQL이다.
-- - application startup에 연결되지 않는다(어떤 코드에서도 이 파일을 참조하지 않는다).
-- - account_id/story_like.account_id/story_comment.account_id는 이 앱의 다른 모든 도메인과 동일하게
--   FK가 없는 느슨한 참조라서(story/story_like/story_comment 어디에도 account 테이블 FK 제약이 없음 —
--   V8/V13/V14 참고), 실제 account row를 만들지 않고 계정 id 범위(9000xx=글 작성자, 9100xx=공감,
--   9200xx=댓글 작성자)를 시드 전용으로 예약해 충돌을 피한다. Story/Comment 응답 모두 작성자 식별
--   필드를 노출하지 않으므로(Story는 익명 원칙, Comment는 lifeStage 표시가 아직 미확정이라 보류 —
--   StoryComment 참고) 기능상 문제가 없다.
-- - like_count는 임의 숫자가 아니라 이 스크립트가 함께 넣는 story_like row 개수와 정확히 일치시켰다
--   (예: 3번째 INSERT의 like_count=3은 아래 story_like에 실제로 3개 row가 들어가는 것과 맞물린다) —
--   그래서 likeCount ↔ 공감 이력 불일치가 없다.
--
-- 로컬 실행 예:
--   docker exec -i skinloop-be-postgres-1 psql -U afterglow -d afterglow < scripts/seed-story-demo.sql
-- (컨테이너/DB 이름은 환경에 맞게 바꾼다. production은 실제 접속 정보로 동일하게 -f/< 실행)
--
-- 주의: idempotent하지 않다 — 두 번 실행하면 같은 글이 중복 생성된다(제목에 unique 제약이 없다).
-- 한 번만 실행한다.

BEGIN;

CREATE TEMP TABLE tmp_seed_story (title TEXT PRIMARY KEY, story_id BIGINT) ON COMMIT DROP;

WITH new_story AS (
    INSERT INTO story (created_at, updated_at, account_id, title, content, life_stage, life_stage_public, like_count, hidden)
    VALUES
        (now(), now(), 900001, '밤에 잠을 설치고 나면 얼굴이 당겨요',
         '요즘 며칠 잠을 설쳤더니 볼이 심하게 당기고 건조해요. 로션을 발라도 금방 당기는 느낌이에요.',
         'PERIMENOPAUSE', true, 3, false),
        (now(), now(), 900002, '스트레스 받으면 턱 주변이 뒤집어져요',
         '일이 몰릴 때마다 턱선 트러블이 심해져요. 관리를 해도 스트레스만 받으면 다시 올라오네요.',
         'MENOPAUSE', true, 5, false),
        (now(), now(), 900003, '환절기만 되면 볼이 따가워요',
         '계절 바뀔 때마다 볼 쪽이 따끔거리고 예민해져서 기초 제품도 조심스러워요.',
         NULL, false, 0, false),
        (now(), now(), 900004, '술 마신 다음날은 얼굴이 빨개져요',
         '전날 술을 마시면 다음날 아침 얼굴 전체가 붉어지고 열감이 있어요.',
         'POST_MENOPAUSE', true, 2, false),
        (now(), now(), 900005, '매운 음식 먹으면 간지러워요',
         '매운 음식이나 커피를 마신 날은 얼굴이 간지럽고 붉은 기가 올라와요.',
         NULL, false, 0, false),
        (now(), now(), 900006, '월경 주기마다 트러블이 올라와요',
         '생리 전후로 항상 턱과 이마에 트러블이 반복돼서 주기를 예상할 수 있을 정도예요.',
         'PERIMENOPAUSE', true, 0, false),
        (now(), now(), 900007, '수면 부족에 스트레스까지 겹치면 건조함이 심해져요',
         '잠도 못 자고 스트레스도 겹친 주에는 얼굴 전체가 푸석하고 당김이 심해요.',
         'MENOPAUSE', false, 0, false),
        (now(), now(), 900008, '커피 마신 날은 붉어지고 가려워요',
         '커피를 마신 날 오후가 되면 얼굴이 붉어지면서 간지러움도 함께 와요.',
         NULL, false, 0, false),
        (now(), now(), 900009, '환절기에 스트레스까지 겹치면 따가움이 심해져요',
         '계절이 바뀌는 시기에 일까지 바쁘면 피부가 더 예민해지고 따가워요.',
         'POST_MENOPAUSE', true, 0, false),
        (now(), now(), 900010, '특별한 계기 없이도 트러블이 반복돼요',
         '딱히 원인을 모르겠는데 트러블과 붉은기가 번갈아 나타나요.',
         NULL, false, 0, false)
    RETURNING id, title
)
INSERT INTO tmp_seed_story (title, story_id)
SELECT title, id FROM new_story;

-- symptomTags(필수, 글마다 1개 이상) — V13 기준 enum(DRYNESS_TIGHTNESS/ITCHING/STINGING/REDNESS/TROUBLE)만 사용.
INSERT INTO story_symptom_tag (story_id, symptom_tag)
SELECT s.story_id, t.tag
FROM tmp_seed_story s
JOIN (VALUES
    ('밤에 잠을 설치고 나면 얼굴이 당겨요', 'DRYNESS_TIGHTNESS'),
    ('스트레스 받으면 턱 주변이 뒤집어져요', 'TROUBLE'),
    ('환절기만 되면 볼이 따가워요', 'STINGING'),
    ('술 마신 다음날은 얼굴이 빨개져요', 'REDNESS'),
    ('매운 음식 먹으면 간지러워요', 'ITCHING'),
    ('월경 주기마다 트러블이 올라와요', 'TROUBLE'),
    ('수면 부족에 스트레스까지 겹치면 건조함이 심해져요', 'DRYNESS_TIGHTNESS'),
    ('커피 마신 날은 붉어지고 가려워요', 'REDNESS'),
    ('커피 마신 날은 붉어지고 가려워요', 'ITCHING'),
    ('환절기에 스트레스까지 겹치면 따가움이 심해져요', 'STINGING'),
    ('특별한 계기 없이도 트러블이 반복돼요', 'TROUBLE'),
    ('특별한 계기 없이도 트러블이 반복돼요', 'REDNESS')
) AS t(title, tag) ON t.title = s.title;

-- situationTags(선택, 0개 이상) — '특별한 계기 없이도...'는 의도적으로 0개(선택 항목임을 함께 보여준다).
INSERT INTO story_situation_tag (story_id, situation_tag)
SELECT s.story_id, t.tag
FROM tmp_seed_story s
JOIN (VALUES
    ('밤에 잠을 설치고 나면 얼굴이 당겨요', 'SLEEP_DEPRIVATION'),
    ('스트레스 받으면 턱 주변이 뒤집어져요', 'STRESS'),
    ('환절기만 되면 볼이 따가워요', 'SEASON_WEATHER'),
    ('술 마신 다음날은 얼굴이 빨개져요', 'ALCOHOL'),
    ('매운 음식 먹으면 간지러워요', 'SPICY_FOOD_COFFEE'),
    ('월경 주기마다 트러블이 올라와요', 'MENSTRUAL_CYCLE'),
    ('수면 부족에 스트레스까지 겹치면 건조함이 심해져요', 'SLEEP_DEPRIVATION'),
    ('수면 부족에 스트레스까지 겹치면 건조함이 심해져요', 'STRESS'),
    ('커피 마신 날은 붉어지고 가려워요', 'SPICY_FOOD_COFFEE'),
    ('환절기에 스트레스까지 겹치면 따가움이 심해져요', 'SEASON_WEATHER'),
    ('환절기에 스트레스까지 겹치면 따가움이 심해져요', 'STRESS')
) AS t(title, tag) ON t.title = s.title;

-- story_like — 위 story.like_count 값과 정확히 같은 개수만큼만 넣는다(계정 id는 9100xx 시드 전용 범위).
INSERT INTO story_like (created_at, updated_at, account_id, story_id)
SELECT now(), now(), t.liker_id, s.story_id
FROM tmp_seed_story s
JOIN (VALUES
    ('밤에 잠을 설치고 나면 얼굴이 당겨요', 910001),
    ('밤에 잠을 설치고 나면 얼굴이 당겨요', 910002),
    ('밤에 잠을 설치고 나면 얼굴이 당겨요', 910003),
    ('스트레스 받으면 턱 주변이 뒤집어져요', 910001),
    ('스트레스 받으면 턱 주변이 뒤집어져요', 910002),
    ('스트레스 받으면 턱 주변이 뒤집어져요', 910003),
    ('스트레스 받으면 턱 주변이 뒤집어져요', 910004),
    ('스트레스 받으면 턱 주변이 뒤집어져요', 910005),
    ('술 마신 다음날은 얼굴이 빨개져요', 910001),
    ('술 마신 다음날은 얼굴이 빨개져요', 910002)
) AS t(title, liker_id) ON t.title = s.title;

-- story_comment(V14) — G2 상세 데모용 최소 seed. 작성자 표시는 아직 없다(lifeStage 파생 규칙 기획
-- 답변 대기, StoryComment 참고) — 그래서 여기서도 임의의 작성자 라벨을 만들지 않는다. 정렬은 오래된 순
-- (Figma G2 확인)이라 VALUES에 쓴 순서 = id 오름차순 그대로 표시 순서가 된다. 계정 id는 9200xx를
-- 시드 전용 댓글 작성자 범위로 예약한다(작성자=9000xx, 공감=9100xx와 겹치지 않게).
INSERT INTO story_comment (created_at, updated_at, story_id, account_id, content)
SELECT now(), now(), s.story_id, t.commenter_id, t.content
FROM tmp_seed_story s
JOIN (VALUES
    ('밤에 잠을 설치고 나면 얼굴이 당겨요', 920001, '저도 요즘 잠을 설치는 날은 얼굴이 유독 당기더라고요.'),
    ('밤에 잠을 설치고 나면 얼굴이 당겨요', 920002, '가습기 틀고 자면 좀 나아지시더라고요.'),
    ('스트레스 받으면 턱 주변이 뒤집어져요', 920001, '저도 스트레스성 트러블 때문에 고생 중이에요.')
) AS t(title, commenter_id, content) ON t.title = s.title;

COMMIT;
