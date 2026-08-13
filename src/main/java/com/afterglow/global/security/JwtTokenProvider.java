package com.afterglow.global.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

	private final SecretKey key;
	private final long expirationMillis;

	public JwtTokenProvider(@Value("${afterglow.jwt.secret}") String secret,
			@Value("${afterglow.jwt.expiration-seconds}") long expirationSeconds) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMillis = expirationSeconds * 1000;
	}

	public String createToken(Long accountId) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMillis);
		return Jwts.builder()
				.subject(String.valueOf(accountId))
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}

	public Long parseAccountId(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		return Long.valueOf(claims.getSubject());
	}
}
