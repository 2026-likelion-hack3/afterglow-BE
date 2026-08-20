package com.afterglow.domain.vanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.afterglow.domain.vanity.CombinationRule;
import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.Product;
import com.afterglow.domain.vanity.RegistrationSource;
import com.afterglow.domain.vanity.UsageTiming;
import com.afterglow.domain.vanity.infrastructure.CombinationRuleRepository;

@ExtendWith(MockitoExtension.class)
class CombinationWarningServiceTest {

	@Mock
	private CombinationRuleRepository combinationRuleRepository;

	@Test
	void 같은_시간대에_충돌_태그가_있으면_경고가_발생한다() {
		CombinationRule rule = CombinationRule.builder()
			.tagA(InteractionTag.RETINOL)
			.tagB(InteractionTag.ACID)
			.minCount(1)
			.warningMessage("레티놀과 산 성분을 같은 시간대에 사용하지 마세요.")
			.build();

		Product retinolProduct = Product.builder()
			.accountId(1L)
			.name("레티놀 제품")
			.usageTiming(UsageTiming.EVENING)
			.interactionTags(Set.of(InteractionTag.RETINOL))
			.registrationSource(RegistrationSource.MANUAL)
			.build();

		Product acidProduct = Product.builder()
			.accountId(1L)
			.name("산 제품")
			.usageTiming(UsageTiming.EVENING)
			.interactionTags(Set.of(InteractionTag.ACID))
			.registrationSource(RegistrationSource.MANUAL)
			.build();

		when(combinationRuleRepository.findAll())
			.thenReturn(List.of(rule));

		CombinationWarningService service =
			new CombinationWarningService(combinationRuleRepository);

		List<String> warnings =
			service.checkWarnings(List.of(retinolProduct, acidProduct));

		assertThat(warnings)
			.containsExactly("레티놀과 산 성분을 같은 시간대에 사용하지 마세요.");
	}

	@Test
	void 충돌_태그라도_사용_시간대가_다르면_경고가_발생하지_않는다() {
		CombinationRule rule = CombinationRule.builder()
			.tagA(InteractionTag.RETINOL)
			.tagB(InteractionTag.ACID)
			.minCount(1)
			.warningMessage("레티놀과 산 성분을 같은 시간대에 사용하지 마세요.")
			.build();

		Product retinolProduct = Product.builder()
			.accountId(1L)
			.name("레티놀 제품")
			.usageTiming(UsageTiming.MORNING)
			.interactionTags(Set.of(InteractionTag.RETINOL))
			.registrationSource(RegistrationSource.MANUAL)
			.build();

		Product acidProduct = Product.builder()
			.accountId(1L)
			.name("산 제품")
			.usageTiming(UsageTiming.EVENING)
			.interactionTags(Set.of(InteractionTag.ACID))
			.registrationSource(RegistrationSource.MANUAL)
			.build();

		when(combinationRuleRepository.findAll())
			.thenReturn(List.of(rule));

		CombinationWarningService service =
			new CombinationWarningService(combinationRuleRepository);

		List<String> warnings =
			service.checkWarnings(List.of(retinolProduct, acidProduct));

		assertThat(warnings).isEmpty();
	}
}
