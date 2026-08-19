package com.afterglow.domain.vanity.api.response;

import java.util.List;

import lombok.Getter;

@Getter
public class ProductCreateResponse {

	private final ProductResponse product;
	private final List<String> warnings;

	public ProductCreateResponse(
		ProductResponse product,
		List<String> warnings) {
		this.product = product;
		this.warnings = warnings;
	}
}
