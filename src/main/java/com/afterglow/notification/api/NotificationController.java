package com.afterglow.notification.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.afterglow.global.security.OpenApiConfig;
import com.afterglow.notification.application.NotificationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class NotificationController {

	private final NotificationService notificationService;

	@PostMapping
	public ResponseEntity<Void> saveOrUpdate(
		@AuthenticationPrincipal Long accountId,
		@Valid @RequestBody NotificationSettingRequest request) {

		notificationService.saveOrUpdate(
			accountId,
			request.type(),
			request.enabled(),
			request.notificationTime(),
			request.timezone()
		);

		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<List<NotificationSettingResponse>> getSettings(
		@AuthenticationPrincipal Long accountId) {

		List<NotificationSettingResponse> responses =
			notificationService.getSettings(accountId)
				.stream()
				.map(NotificationSettingResponse::from)
				.toList();

		return ResponseEntity.ok(responses);
	}
}
