package com.afterglow.domain.vanity.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.afterglow.domain.vanity.api.request.OcrTextRequest;
import com.afterglow.domain.vanity.api.request.ProductCreateRequest;
import com.afterglow.domain.vanity.api.response.ProductCreateResponse;
import com.afterglow.domain.vanity.api.response.ProductRegistrationDraftResponse;
import com.afterglow.domain.vanity.api.response.ProductResponse;
import com.afterglow.domain.vanity.application.ProductRegistrationDraftResult;
import com.afterglow.domain.vanity.application.ProductRegistrationDraftService;
import com.afterglow.domain.vanity.application.VanityService;
import com.afterglow.domain.vanity.api.response.OcrResponse;
import com.afterglow.domain.vanity.application.VanityOcrService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vanity/products")
public class VanityController {

	private final VanityService vanityService;
	private final VanityOcrService vanityOcrService;
	private final ProductRegistrationDraftService productRegistrationDraftService;

	@PostMapping
	public ResponseEntity<ProductCreateResponse> createProduct(
		@AuthenticationPrincipal Long accountId,
		@Valid @RequestBody ProductCreateRequest request) {

		ProductCreateResponse response =
			vanityService.createProduct(accountId, request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<ProductResponse>> getProducts(
		@AuthenticationPrincipal Long accountId) {

		return ResponseEntity.ok(
			vanityService.getProducts(accountId)
		);
	}

	@GetMapping("/{productId}")
	public ResponseEntity<ProductResponse> getProduct(
		@AuthenticationPrincipal Long accountId,
		@PathVariable Long productId) {

		return ResponseEntity.ok(
			vanityService.getProduct(accountId, productId)
		);
	}

	@PatchMapping("/{productId}")
	public ResponseEntity<ProductResponse> updateProduct(
		@AuthenticationPrincipal Long accountId,
		@PathVariable Long productId,
		@Valid @RequestBody ProductCreateRequest request) {

		return ResponseEntity.ok(
			vanityService.updateProduct(
				accountId,
				productId,
				request
			)
		);
	}

	@DeleteMapping("/{productId}")
	public ResponseEntity<Void> deleteProduct(
		@AuthenticationPrincipal Long accountId,
		@PathVariable Long productId) {

		vanityService.deleteProduct(accountId, productId);

		return ResponseEntity.noContent().build();
	}

	@PostMapping(
		value = "/ocr",
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ResponseEntity<OcrResponse> extractOcrText(
		@RequestPart("image") MultipartFile image) {

		String rawText = vanityOcrService.extractRawText(image);

		return ResponseEntity.ok(
			new OcrResponse(rawText)
		);
	}

	@PostMapping("/ocr/structure")
	public ResponseEntity<ProductRegistrationDraftResponse> structureOcrText(
		@Valid @RequestBody OcrTextRequest request) {

		ProductRegistrationDraftResult result =
			productRegistrationDraftService.createDraft(request.rawText());

		return ResponseEntity.ok(
			ProductRegistrationDraftResponse.from(result)
		);
	}
}
