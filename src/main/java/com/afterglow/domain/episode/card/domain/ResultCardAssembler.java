package com.afterglow.domain.episode.card.domain;

import java.util.List;

import com.afterglow.domain.episode.analysis.domain.AnalysisResult;
import com.afterglow.domain.episode.analysis.domain.CandidateResult;
import com.afterglow.domain.episode.analysis.domain.CandidateType;
import com.afterglow.domain.episode.analysis.domain.ExclusionReason;

/**
 * Manyfast F-HGUJDZ(결과 카드 3장) 확정 규칙을 그대로 구현한 결정적(pure) 조립 로직. {@link AnalysisResult}
 * 만으로 동작하며 Repository/외부 API/Vanity/Story에 의존하지 않는다 — 같은 입력이면 항상 같은 결과를
 * 반환한다.
 *
 * <p>규칙:
 * <ul>
 *   <li>항상 카드 3장: (1) 원인 후보 관련 카드, (2) 오늘 사용할 것, (3) 병원 방문 기준. 세 번째는
 *       hold 여부와 무관하게 항상 포함된다.</li>
 *   <li>HOLD면 첫 번째 카드가 {@code WITHHELD}로 대체된다.</li>
 *   <li>HOLD가 아니고 top 후보 타입이 {@code WEATHER}면("멈출 대상이 없으므로") 첫 번째 카드가
 *       {@code DISCONTINUE} 대신 {@code DO_MORE_TODAY}가 된다.</li>
 *   <li>두 번째 카드({@code CONTINUE_USE})는 보유 제품 전체 목록(Vanity)이 있어야 실제 항목을 채울 수
 *       있어, 이번 범위에서는 카드 자리만 만들고 내용은 비워둔다.</li>
 * </ul>
 */
public final class ResultCardAssembler {

	public ResultCardResult assemble(AnalysisResult analysisResult) {
		ResultCard continueUseCard = new ResultCard(ResultCardType.CONTINUE_USE, null, null, null);
		ResultCard hospitalCard = new ResultCard(ResultCardType.HOSPITAL_VISIT, null, null, null);

		if (analysisResult.hold()) {
			ResultCard withheldCard = new ResultCard(ResultCardType.WITHHELD, null, null, null);
			ResultCardHoldReason holdReason = deriveHoldReason(analysisResult);
			return ResultCardResult.hold(holdReason, List.of(withheldCard, continueUseCard, hospitalCard));
		}

		CandidateResult top = analysisResult.topCandidate();
		ResultCardType firstCardType = top.type() == CandidateType.WEATHER
				? ResultCardType.DO_MORE_TODAY
				: ResultCardType.DISCONTINUE;
		ResultCard firstCard = new ResultCard(firstCardType, top.type(), top.evidence(), top.coverageDays());

		return ResultCardResult.determined(analysisResult.confidence(), List.of(firstCard, continueUseCard, hospitalCard));
	}

	/**
	 * "보류 화면 문구는 제외 사유를 보고 고른다. 기록부족이면 며칠 더 모이면 알려드릴 수 있다고 말하고,
	 * 대상없음이면 그런 약속을 하지 않는다." — 후보 목록이 비어있을 때(모든 타입이 게이트에서 제외됨)만
	 * 이 구분을 적용한다. 후보는 있었지만 근거 부족/동점/gap으로 보류된 경우는 별도 사유로 분류한다.
	 */
	private ResultCardHoldReason deriveHoldReason(AnalysisResult analysisResult) {
		if (analysisResult.candidates().isEmpty()) {
			boolean anyInsufficientRecords = analysisResult.exclusions().stream()
					.anyMatch(exclusion -> exclusion.reason() == ExclusionReason.INSUFFICIENT_RECORDS);
			return anyInsufficientRecords ? ResultCardHoldReason.INSUFFICIENT_RECORDS : ResultCardHoldReason.NO_TARGET;
		}
		return ResultCardHoldReason.INCONCLUSIVE_EVIDENCE;
	}
}
