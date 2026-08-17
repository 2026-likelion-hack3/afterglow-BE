package com.afterglow.vanity.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.afterglow.vanity.Product;
import com.afterglow.vanity.api.request.ProductCreateRequest;
import com.afterglow.vanity.api.response.ProductCreateResponse;
import com.afterglow.vanity.api.response.ProductResponse;
import com.afterglow.vanity.infrastructure.ProductRepository;

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

	public List<ProductResponse> getProducts(Long accountId) {
		return productRepository.findAllByAccountId(accountId)
			.stream()
			.map(ProductResponse::new)
			.toList();
	}

	public ProductResponse getProduct(Long accountId, Long productId) {
		Product product = productRepository.findByIdAndAccountId(productId, accountId)
			.orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다."));

		return new ProductResponse(product);
	}

	@Transactional
	public ProductResponse updateProduct(
		Long accountId,
		Long productId,
		ProductCreateRequest request) {

		Product product = productRepository.findByIdAndAccountId(productId, accountId)
			.orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다."));

		product.updateDetails(
			request.getName(),
			request.getBrand(),
			request.getType(),
			request.getKeyIngredients(),
			request.getFunctionTags(),
			request.getOpenedAt(),
			request.getUsageTiming(),
			request.getInteractionTags()
		);

		return new ProductResponse(product);
	}

	@Transactional
	public void deleteProduct(Long accountId, Long productId) {

		Product product = productRepository.findByIdAndAccountId(productId, accountId)
			.orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다."));

		productRepository.delete(product);
	}
}
