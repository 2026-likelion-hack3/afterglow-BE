package com.afterglow.domain.vanity.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.vanity.infrastructure.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VanityQueryService {

	private final ProductRepository productRepository;

	public List<VanityQueryResponse> findProductsByAccountId(Long accountId) {
		return productRepository.findAllByAccountId(accountId)
			.stream()
			.map(product -> new VanityQueryResponse(
				product.getId(),
				product.getOpenedAt(),
				product.getOpeningPeriod(),
				product.getCreatedAt(),
				product.getInteractionTags(),
				product.getUsageTiming(),
				product.getUsageTimingChangedAt()
			))
			.toList();
	}
}
