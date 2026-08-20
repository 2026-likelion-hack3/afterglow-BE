package com.afterglow.domain.vanity.infrastructure;

public interface VisionOcrClient {

	String extractText(byte[] imageBytes);
}
