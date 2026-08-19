package com.afterglow.domain.vanity.api.request;

import java.time.LocalDate;
import java.util.Set;

import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.domain.vanity.RegistrationSource;
import com.afterglow.domain.vanity.UsageTiming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductCreateRequest {

	@NotBlank
	private String name;

	private String brand;

	private String type;

	private String keyIngredients;

	private Set<String> functionTags;

	private LocalDate openedAt;

	private UsageTiming usageTiming;

	private Set<InteractionTag> interactionTags;

	@NotNull
	private RegistrationSource registrationSource;

	private String barcode;

	private String photoKey;
}
