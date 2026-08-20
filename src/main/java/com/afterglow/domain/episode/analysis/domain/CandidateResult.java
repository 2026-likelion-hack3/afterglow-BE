package com.afterglow.domain.episode.analysis.domain;

/**
 * 근거 강도까지 계산된 후보 하나.
 *
 * @param coverageDays 이 후보의 근거 기간 — card가 "N일치 기록에 근거" 표시에 쓴다.
 *                     {@link FrequencyEvidence}의 관찰 횟수와는 다른 값이라 섞지 않는다. SLEEP/WEATHER는
 *                     {@link RecordCoverage#coverageDays}(최초 기록일~분석 기준일 calendar-day 커버리지,
 *                     기존 규칙 그대로), PRODUCT/COMBINATION은 관측 window 안에 실제 CheckIn이 존재하는
 *                     날짜 수({@link ProductObservationWindow#coverageDays}, 2026-08-20 신설)다 — 둘 다
 *                     항상 값을 갖는다(2026-08-18에는 PRODUCT/COMBINATION이 null이었으나 이후 정정됨).
 */
public record CandidateResult(CandidateType type, EvidenceStrength strength, Evidence evidence, Long coverageDays) {
}
