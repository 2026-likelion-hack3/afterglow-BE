package com.afterglow.domain.vanity.application;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.afterglow.domain.vanity.infrastructure.VisionOcrClient;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VanityOcrService {

	private final VisionOcrClient visionOcrClient;

	public String extractRawText(MultipartFile image) {

		if (image == null || image.isEmpty()) {
			throw new AfterglowException(
				ErrorCode.INVALID_REQUEST,
				"OCR 이미지가 필요합니다."
			);
		}

		try {
			return visionOcrClient.extractText(image.getBytes());
		} catch (Exception e) {
			return "";
		}
	}
}
