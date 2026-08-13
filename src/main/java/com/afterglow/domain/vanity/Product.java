package com.afterglow.domain.vanity;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.afterglow.global.BaseEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

	@Column(nullable = false)
	private Long accountId;

	@Column(nullable = false)
	private String name;

	private String brand;

	/** 자유 형식 — 토너/크림/앰플 등 유형 체계가 명세서에 고정돼 있지 않다 */
	private String type;

	private String keyIngredients;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "product_function_tag", joinColumns = @JoinColumn(name = "product_id"))
	@Column(name = "tag")
	private Set<String> functionTags = new LinkedHashSet<>();

	private LocalDate openedAt;

	@Enumerated(EnumType.STRING)
	private UsageTiming usageTiming;

	/** 5개 상호작용 태그로 한정 — 기능명세서 5.1 비즈니스 규칙 */
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "product_interaction_tag", joinColumns = @JoinColumn(name = "product_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "tag")
	private Set<InteractionTag> interactionTags = new LinkedHashSet<>();

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private RegistrationSource registrationSource;

	/** 바코드로 등록한 경우에만 값을 가진다 */
	private String barcode;

	/** 사진으로 등록한 경우 제품 전면 사진의 S3 키 */
	private String photoKey;

	@Builder
	private Product(Long accountId, String name, String brand, String type, String keyIngredients,
			Set<String> functionTags, LocalDate openedAt, UsageTiming usageTiming,
			Set<InteractionTag> interactionTags, RegistrationSource registrationSource,
			String barcode, String photoKey) {
		this.accountId = accountId;
		this.name = name;
		this.brand = brand;
		this.type = type;
		this.keyIngredients = keyIngredients;
		if (functionTags != null) {
			this.functionTags = functionTags;
		}
		this.openedAt = openedAt;
		this.usageTiming = usageTiming;
		if (interactionTags != null) {
			this.interactionTags = interactionTags;
		}
		this.registrationSource = registrationSource;
		this.barcode = barcode;
		this.photoKey = photoKey;
	}

	public void updateDetails(String name, String brand, String type, String keyIngredients,
			Set<String> functionTags, LocalDate openedAt, UsageTiming usageTiming,
			Set<InteractionTag> interactionTags) {
		this.name = name;
		this.brand = brand;
		this.type = type;
		this.keyIngredients = keyIngredients;
		this.functionTags = functionTags != null ? functionTags : new LinkedHashSet<>();
		this.openedAt = openedAt;
		this.usageTiming = usageTiming;
		this.interactionTags = interactionTags != null ? interactionTags : new LinkedHashSet<>();
	}
}
