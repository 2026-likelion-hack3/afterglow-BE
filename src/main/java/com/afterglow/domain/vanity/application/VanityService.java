package com.afterglow.domain.vanity.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.domain.vanity.Product;
import com.afterglow.domain.vanity.api.request.ProductCreateRequest;
import com.afterglow.domain.vanity.api.response.ProductCreateResponse;
import com.afterglow.domain.vanity.api.response.ProductResponse;
import com.afterglow.domain.vanity.infrastructure.ProductRepository;
import com.afterglow.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VanityService {

	private final ProductRepository productRepository;
	private final CombinationWarningService combinationWarningService;

	@Transactional
	public ProductCreateResponse createProduct(Long accountId, ProductCreateRequest request) {

		Product product = Product.builder()
			.accountId(accountId)
			.name(request.getName())
			.brand(request.getBrand())
			.type(request.getType())
			.keyIngredients(request.getKeyIngredients())
			.functionTags(request.getFunctionTags())
			.openedAt(request.getOpenedAt())
			.openingPeriod(request.getOpeningPeriod())
			.usageTiming(request.getUsageTiming())
			.interactionTags(request.getInteractionTags())
			.registrationSource(request.getRegistrationSource())
			.barcode(request.getBarcode())
			.photoKey(request.getPhotoKey())
			.build();

		Product savedProduct = productRepository.save(product);

		List<Product> products = productRepository.findAllByAccountId(accountId);

		List<String> warnings = combinationWarningService.checkWarnings(products);

		return new ProductCreateResponse(
			new ProductResponse(savedProduct),
			warnings
		);
	}

	/** 2026-08-20: 기존 top-level shape(배열) 유지 — 프론트 호환성. warnings는 별도 endpoint({@link #getCombinationWarnings})로 분리했다. */
	public List<ProductResponse> getProducts(Long accountId) {
		return productRepository.findAllByAccountId(accountId).stream()
			.map(ProductResponse::new)
			.toList();
	}

	/** 2026-08-20: 기존 top-level shape(ProductResponse 단일 객체) 유지 — 프론트 호환성. warnings는 별도 endpoint로 분리했다. */
	public ProductResponse getProduct(Long accountId, Long productId) {
		Product product = productRepository.findByIdAndAccountId(productId, accountId)
			.orElseThrow(() -> new NotFoundException("제품을 찾을 수 없습니다."));

		return new ProductResponse(product);
	}

	/**
	 * 계정 전체 보유 제품 기준 조합 경고(제품별로 나뉘지 않음) — 기존 POST 생성 흐름과 동일한
	 * {@code checkWarnings} 호출을 그대로 재사용한다(2026-08-20, 새 warning 판정 규칙 추가 없음).
	 * 어떤 제품이 경고를 유발했는지 매핑하는 기능은 {@code CombinationWarningService}가 지원하지
	 * 않아 이번 범위에서 만들지 않았다 — known mismatch로 남겨둔다.
	 */
	public List<String> getCombinationWarnings(Long accountId) {
		List<Product> products = productRepository.findAllByAccountId(accountId);
		return combinationWarningService.checkWarnings(products);
	}

	@Transactional
	public ProductResponse updateProduct(
		Long accountId,
		Long productId,
		ProductCreateRequest request) {

		Product product = productRepository.findByIdAndAccountId(productId, accountId)
			.orElseThrow(() -> new NotFoundException("제품을 찾을 수 없습니다."));

		product.updateDetails(
			request.getName(),
			request.getBrand(),
			request.getType(),
			request.getKeyIngredients(),
			request.getFunctionTags(),
			request.getOpenedAt(),
			request.getOpeningPeriod(),
			request.getUsageTiming(),
			request.getInteractionTags()
		);

		return new ProductResponse(product);
	}

	@Transactional
	public void deleteProduct(Long accountId, Long productId) {

		Product product = productRepository.findByIdAndAccountId(productId, accountId)
			.orElseThrow(() -> new NotFoundException("제품을 찾을 수 없습니다."));

		productRepository.delete(product);
	}
}
