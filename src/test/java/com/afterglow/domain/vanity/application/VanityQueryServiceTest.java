package com.afterglow.domain.vanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.OpeningPeriod;
import com.afterglow.domain.vanity.Product;
import com.afterglow.domain.vanity.RegistrationSource;
import com.afterglow.domain.vanity.UsageTiming;
import com.afterglow.domain.vanity.infrastructure.ProductRepository;

@ExtendWith(MockitoExtension.class)
class VanityQueryServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Test
	void 계정의_제품_raw_data를_조회한다() {
		Product product = Product.builder()
			.accountId(1L)
			.name("테스트 제품")
			.openedAt(LocalDate.of(2026, 8, 1))
			.openingPeriod(OpeningPeriod.ONE_TO_THREE_MONTHS)
			.usageTiming(UsageTiming.EVENING)
			.interactionTags(Set.of(InteractionTag.RETINOL))
			.registrationSource(RegistrationSource.MANUAL)
			.build();

		when(productRepository.findAllByAccountId(1L))
			.thenReturn(List.of(product));

		VanityQueryService service =
			new VanityQueryService(productRepository);

		List<VanityQueryResponse> result =
			service.findProductsByAccountId(1L);

		assertThat(result).hasSize(1);

		VanityQueryResponse response = result.get(0);

		assertThat(response.openedAt())
			.isEqualTo(LocalDate.of(2026, 8, 1));

		assertThat(response.openingPeriod())
			.isEqualTo(OpeningPeriod.ONE_TO_THREE_MONTHS);

		assertThat(response.usageTiming())
			.isEqualTo(UsageTiming.EVENING);

		assertThat(response.interactionTags())
			.containsExactly(InteractionTag.RETINOL);
	}
}
