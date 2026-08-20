package com.afterglow.domain.episode.analysis.application;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.afterglow.domain.episode.analysis.domain.AnalysisExplanation;
import com.afterglow.domain.episode.analysis.domain.AnalysisExplanationFallback;
import com.afterglow.domain.episode.analysis.domain.AnalysisExplanationGenerator;
import com.afterglow.domain.episode.analysis.domain.AnalysisExplanationOutcome;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResult;
import com.afterglow.domain.episode.analysis.domain.EpisodeAnalysisResultRepository;
import com.afterglow.domain.episode.analysis.domain.ExplanationSource;
import com.afterglow.domain.episode.card.domain.ResultCardResult;
import com.afterglow.domain.episode.domain.EpisodeRepository;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;
import com.afterglow.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * 이미 저장된 {@link EpisodeAnalysisResult}를 문장으로 설명한다 — 새 분석을 실행하거나 저장된 값을
 * 바꾸지 않는다(읽기 전용). {@link AnalysisExplanationGenerator} 빈이 없거나(enabled=false) 호출이
 * {@link ErrorCode#AI_REQUEST_FAILED}로 실패하면 {@link AnalysisExplanationFallback}으로 넘어간다 —
 * 이 서비스는 어떤 경우에도 예외를 던져 API를 500/503으로 만들지 않는다(ownership/미존재 오류는 예외).
 */
@Service
@RequiredArgsConstructor
public class EpisodeAnalysisExplanationService {

	private static final Logger log = LoggerFactory.getLogger(EpisodeAnalysisExplanationService.class);

	private final EpisodeRepository episodeRepository;
	private final EpisodeAnalysisResultRepository episodeAnalysisResultRepository;
	private final Optional<AnalysisExplanationGenerator> analysisExplanationGenerator;
	private final AnalysisExplanationFallback fallback = new AnalysisExplanationFallback();

	public AnalysisExplanationOutcome explain(Long accountId, Long episodeId) {
		requireOwnedEpisode(accountId, episodeId);
		ResultCardResult analysisResult = loadAnalysisResult(accountId, episodeId);

		if (analysisExplanationGenerator.isPresent()) {
			try {
				AnalysisExplanation explanation = analysisExplanationGenerator.get().generate(analysisResult);
				return new AnalysisExplanationOutcome(explanation, ExplanationSource.AI);
			} catch (AfterglowException e) {
				if (e.getErrorCode() != ErrorCode.AI_REQUEST_FAILED) {
					throw e;
				}
				log.warn("AI 설명 생성 실패 — 결정적 fallback으로 전환합니다. episodeId={}", episodeId);
			}
		}

		return new AnalysisExplanationOutcome(fallback.explain(analysisResult), ExplanationSource.FALLBACK);
	}

	private ResultCardResult loadAnalysisResult(Long accountId, Long episodeId) {
		return episodeAnalysisResultRepository.findByEpisodeIdAndAccountId(episodeId, accountId)
				.map(EpisodeAnalysisResult::toResultCardResult)
				.orElseThrow(() -> new NotFoundException("분석 결과를 찾을 수 없습니다."));
	}

	private void requireOwnedEpisode(Long accountId, Long episodeId) {
		episodeRepository.findByIdAndAccountId(episodeId, accountId)
				.orElseThrow(() -> new NotFoundException("에피소드를 찾을 수 없습니다."));
	}
}
