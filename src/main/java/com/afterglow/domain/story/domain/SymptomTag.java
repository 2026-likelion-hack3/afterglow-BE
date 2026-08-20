package com.afterglow.domain.story.domain;

/**
 * 이야기 글의 증상 태그 — Manyfast dataSpec(건조·당김/가려움/따가움/붉어짐/트러블) 및 V8 마이그레이션의
 * DB CHECK 제약과 값 이름을 맞춘다(2026-08-20 확정). 이전에는 {@code DRYNESS}/{@code BREAKOUT}이라는
 * 다른 이름을 썼는데, DB CHECK 제약은 처음부터 {@code DRYNESS_TIGHTNESS}/{@code TROUBLE}만 허용해서
 * 실제로 이 두 값을 골라 글을 쓰면 DB 저장 시점에 CHECK 제약 위반으로 실패했다(테스트가 이를 못 잡은
 * 이유는 {@code @Transactional} 롤백이 flush 전에 일어났기 때문). 이 enum 이름 자체가 DB 계약이므로
 * 앞으로도 이 값을 임의로 바꾸지 않는다.
 */
public enum SymptomTag {
	DRYNESS_TIGHTNESS,
	ITCHING,
	STINGING,
	REDNESS,
	TROUBLE
}
