package com.afterglow.domain.episode.card.domain;

/** 결과 카드 한 장의 역할 — 기능명세서 3.1(Manyfast F-HGUJDZ). */
public enum ResultCardType {
	/** 판단 보류 상태에서 첫 번째 카드 자리를 대체한다("아직 판단하기 이릅니다"). */
	WITHHELD,
	/** 오늘 중단할 것 — 원인 후보가 WEATHER가 아닐 때의 첫 번째 카드. */
	DISCONTINUE,
	/** 오늘 더 해줄 것 — 원인 후보가 WEATHER일 때는 멈출 대상이 없어 DISCONTINUE 대신 이 타입을 쓴다. */
	DO_MORE_TODAY,
	/** 오늘 사용할 것 — 두 번째 카드. 보유 제품 전체 목록이 있어야 실제 항목을 채울 수 있어(Vanity 필요)
	 *  이번 범위에서는 카드 자리만 존재하고 내용은 비어 있다. */
	CONTINUE_USE,
	/** 병원에 가야 하는 신호 — 세 번째 카드, 원인 후보/확신 단계와 무관하게 항상 포함된다. */
	HOSPITAL_VISIT
}
