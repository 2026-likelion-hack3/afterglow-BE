package com.afterglow.global.security;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/** Swagger UI Authorize 버튼에 쓰이는 JWT bearer security scheme을 등록한다. 개별 API 적용은 각 컨트롤러의 @SecurityRequirement(name = BEARER_AUTH)로 한다. */
@Configuration
@SecurityScheme(
		name = OpenApiConfig.BEARER_AUTH,
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT",
		in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearerAuth";
}
