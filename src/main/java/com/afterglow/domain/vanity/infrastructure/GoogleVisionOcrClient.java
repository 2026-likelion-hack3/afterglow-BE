package com.afterglow.domain.vanity.infrastructure;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;

@Component
public class GoogleVisionOcrClient implements VisionOcrClient {

	private static final Duration OCR_TIMEOUT = Duration.ofSeconds(10);

	@Override
	public String extractText(byte[] imageBytes) {

		if (imageBytes == null || imageBytes.length == 0) {
			return "";
		}

		try {
			ImageAnnotatorSettings settings = createSettings();

			try (ImageAnnotatorClient client =
					 ImageAnnotatorClient.create(settings)) {

				Image image = Image.newBuilder()
					.setContent(ByteString.copyFrom(imageBytes))
					.build();

				Feature feature = Feature.newBuilder()
					.setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
					.build();

				AnnotateImageRequest request =
					AnnotateImageRequest.newBuilder()
						.setImage(image)
						.addFeatures(feature)
						.build();

				BatchAnnotateImagesResponse batchResponse =
					client.batchAnnotateImages(
						List.of(request)
					);

				AnnotateImageResponse response =
					batchResponse.getResponses(0);

				if (response.hasError()) {
					return "";
				}

				if (!response.hasFullTextAnnotation()) {
					return "";
				}

				return response
					.getFullTextAnnotation()
					.getText();
			}

		} catch (Exception e) {
			return "";
		}
	}

	private ImageAnnotatorSettings createSettings()
		throws IOException {

		ImageAnnotatorSettings.Builder builder =
			ImageAnnotatorSettings.newBuilder();

		RetrySettings retrySettings =
			builder.batchAnnotateImagesSettings()
				.getRetrySettings()
				.toBuilder()
				.setInitialRpcTimeoutDuration(OCR_TIMEOUT)
				.setMaxRpcTimeoutDuration(OCR_TIMEOUT)
				.setTotalTimeoutDuration(OCR_TIMEOUT)
				.build();

		builder.batchAnnotateImagesSettings()
			.setRetrySettings(retrySettings);

		return builder.build();
	}
}
