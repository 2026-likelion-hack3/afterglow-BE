package com.afterglow.vanity.application;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.afterglow.vanity.CombinationRule;
import com.afterglow.vanity.InteractionTag;
import com.afterglow.vanity.Product;
import com.afterglow.vanity.UsageTiming;
import com.afterglow.vanity.infrastructure.CombinationRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CombinationWarningService {

	private final CombinationRuleRepository combinationRuleRepository;

	public List<String> checkWarnings(List<Product> products) {
		List<String> warnings = new ArrayList<>();

		for (CombinationRule rule : combinationRuleRepository.findAll()) {
			if (rule.isSameTagThresholdRule()) {
				checkSameTagThreshold(products, rule, warnings);
			} else {
				checkTagCombination(products, rule, warnings);
			}
		}

		return warnings;
	}

	private void checkSameTagThreshold(
		List<Product> products,
		CombinationRule rule,
		List<String> warnings) {

		for (UsageTiming timing : new UsageTiming[]{
			UsageTiming.MORNING,
			UsageTiming.EVENING
		}) {
			long count = products.stream()
				.filter(product -> isUsedAt(product.getUsageTiming(), timing))
				.filter(product -> product.getInteractionTags()
					.contains(rule.getTagA()))
				.count();

			if (count >= rule.getMinCount()) {
				warnings.add(rule.getWarningMessage());
			}
		}
	}

	private void checkTagCombination(
		List<Product> products,
		CombinationRule rule,
		List<String> warnings) {

		boolean hasWarning = false;

		for (UsageTiming timing : new UsageTiming[]{
			UsageTiming.MORNING,
			UsageTiming.EVENING
		}) {
			boolean hasTagA = products.stream()
				.filter(product -> isUsedAt(product.getUsageTiming(), timing))
				.anyMatch(product -> product.getInteractionTags()
					.contains(rule.getTagA()));

			boolean hasTagB = products.stream()
				.filter(product -> isUsedAt(product.getUsageTiming(), timing))
				.anyMatch(product -> product.getInteractionTags()
					.contains(rule.getTagB()));

			if (hasTagA && hasTagB) {
				hasWarning = true;
				break;
			}
		}

		if (hasWarning) {
			warnings.add(rule.getWarningMessage());
		}
	}

	private boolean isUsedAt(UsageTiming productTiming, UsageTiming targetTiming) {
		if (productTiming == null) {
			return false;
		}

		return productTiming == UsageTiming.BOTH
			|| productTiming == targetTiming;
	}
}
