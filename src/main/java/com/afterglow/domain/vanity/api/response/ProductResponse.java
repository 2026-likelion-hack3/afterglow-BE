package com.afterglow.domain.vanity.api.response;

import java.time.LocalDate;
import java.util.Set;

import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.Product;
import com.afterglow.domain.vanity.RegistrationSource;
import com.afterglow.domain.vanity.UsageTiming;

import lombok.Getter;

@Getter
public class ProductResponse {

	private final Long id;
	private final String name;
	private final String brand;
	private final String type;
	private final String keyIngredients;
	private final Set<String> functionTags;
	private final LocalDate openedAt;
	private final UsageTiming usageTiming;
	private final Set<InteractionTag> interactionTags;
	private final RegistrationSource registrationSource;
	private final String barcode;
	private final String photoKey;

	public ProductResponse(Product product) {
		this.id = product.getId();
		this.name = product.getName();
		this.brand = product.getBrand();
		this.type = product.getType();
		this.keyIngredients = product.getKeyIngredients();
		this.functionTags = product.getFunctionTags();
		this.openedAt = product.getOpenedAt();
		this.usageTiming = product.getUsageTiming();
		this.interactionTags = product.getInteractionTags();
		this.registrationSource = product.getRegistrationSource();
		this.barcode = product.getBarcode();
		this.photoKey = product.getPhotoKey();
	}
}
