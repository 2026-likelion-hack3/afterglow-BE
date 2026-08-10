package com.afterglow.account.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/** EC2 인스턴스 IAM role 등 기본 자격증명 체인을 사용한다 — 별도 키를 설정에 넣지 않는다. */
@Configuration
@Profile("prod")
public class SesClientConfig {

	@Bean
	public SesClient sesClient(@Value("${afterglow.aws.region}") String region) {
		return SesClient.builder()
				.region(Region.of(region))
				.credentialsProvider(DefaultCredentialsProvider.builder().build())
				.build();
	}
}
