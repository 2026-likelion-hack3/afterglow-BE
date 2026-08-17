package com.afterglow.domain.episode.card.domain;

import com.afterglow.domain.episode.analysis.domain.CandidateType;
import com.afterglow.domain.episode.analysis.domain.Evidence;

/**
 * 카드 한 장. Manyfast dataSpec의 "제목/대상 제품 또는 기준/이유 한 줄/근거 출처" 중, 실제 문장(제목,
 * 이유 한 줄)은 만들지 않는다 — 제품명 등 렌더링에 필요한 데이터가 Vanity에 있어야 하는데 아직 연동이
 * 없기 때문이다. 대신 문장을 조립할 수 있는 구조화된 근거(causeType/evidence/coverageDays, 기존
 * Analysis 출력 재사용)만 담는다.
 *
 * @param type          이 카드의 역할
 * @param causeType     이 카드가 참조하는 원인 후보 타입 — 후보를 참조하지 않는 카드(CONTINUE_USE,
 *                      HOSPITAL_VISIT, WITHHELD)는 null
 * @param evidence      원인 후보의 근거(기존 Analysis {@link Evidence} 재사용) — causeType이 null이면 null
 * @param coverageDays  근거가 된 기록 일수("N일치 기록") — causeType이 null이면 null
 */
public record ResultCard(ResultCardType type, CandidateType causeType, Evidence evidence, Long coverageDays) {
}
