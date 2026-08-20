package com.afterglow.domain.vanity.api.request;

import jakarta.validation.constraints.NotBlank;

public record OcrTextRequest(
		@NotBlank
		String rawText
) {
}
