package com.afterglow.global.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	/** 인증 없이 호출 가능한 경로. 새 도메인에서 인증 없는 API가 필요하면 여기에 추가한다. */
	private static final String[] PERMIT_ALL_PATHS = {
			"/api/accounts/anonymous",
			"/api/accounts/login/**",
			"/swagger-ui/**",
			"/v3/api-docs/**",
			"/actuator/health",
	};

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	/** comma-separated 원본 값. {@link #corsConfigurationSource()}에서 직접 split/trim해서 각각의 Origin으로 등록한다. */
	@Value("${afterglow.cors.allowed-origins}")
	private String corsAllowedOrigins;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// preflight는 브라우저가 인증 헤더 없이 보내므로, 실제 API 인가와 별개로 항상 통과시킨다.
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(PERMIT_ALL_PATHS).permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(handler -> handler.authenticationEntryPoint(jwtAuthenticationEntryPoint))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	private CorsConfigurationSource corsConfigurationSource() {
		List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList();

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
