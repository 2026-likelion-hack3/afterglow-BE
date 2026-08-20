package com.afterglow.domain.vanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.afterglow.domain.vanity.InteractionTag;
import com.afterglow.global.exception.AfterglowException;
import com.afterglow.global.exception.ErrorCode;

/**
 * 실제 OpenAI를 호출하지 않는다 — {@link ProductRegistrationDraftGenerator}는 람다로 직접 구현해 원하는
 * 성공/실패를 결정적으로 재현한다({@code EpisodeAnalysisExplanationServiceTest}와 동일한 관례).
 * interactionTags는 AI가 아니라 {@link InteractionTagMatcher}(결정적 성분명 매칭)로 채워지므로, generator가
 * 어떤 draft를 반환하든 keyIngredients 문자열만으로 매칭 여부가 결정된다.
 */
class ProductRegistrationDraftServiceTest {

	@Test
	void 정상_rawText면_generator가_반환한_draft를_그대로_돌려준다() {
		ProductRegistrationDraftGenerator generator = rawText ->
				new ProductRegistrationDraft("촉촉 크림", "글로우브랜드", "크림", "정제수, 글리세린, 나이아신아마이드");
		ProductRegistrationDraftService service = serviceWith(Optional.of(generator));

		ProductRegistrationDraftResult result = service.createDraft("촉촉 크림 글로우브랜드 정제수, 글리세린, 나이아신아마이드");

		assertThat(result.draft().name()).isEqualTo("촉촉 크림");
		assertThat(result.draft().brand()).isEqualTo("글로우브랜드");
		assertThat(result.draft().type()).isEqualTo("크림");
		assertThat(result.draft().keyIngredients()).isEqualTo("정제수, 글리세린, 나이아신아마이드");
	}

	@Test
	void OCR_텍스트에_없는_값은_generator가_null로_남겨도_그대로_전달된다() {
		ProductRegistrationDraftGenerator generator = rawText ->
				new ProductRegistrationDraft(null, null, null, "정제수, 글리세린");
		ProductRegistrationDraftService service = serviceWith(Optional.of(generator));

		ProductRegistrationDraftResult result = service.createDraft("정제수, 글리세린");

		assertThat(result.draft().name()).isNull();
		assertThat(result.draft().brand()).isNull();
		assertThat(result.draft().type()).isNull();
		assertThat(result.draft().keyIngredients()).isEqualTo("정제수, 글리세린");
	}

	@Test
	void draft_타입에는_OpeningPeriod나_UsageTiming_필드_자체가_없다() {
		// 컴파일 타임 보증: ProductRegistrationDraft는 name/brand/type/keyIngredients 네 필드만 가진다.
		// AI가 개봉일/사용시점 같은 사용자 행동 정보를 채워 넣을 방법 자체가 타입 수준에서 없다.
		assertThat(ProductRegistrationDraft.class.getRecordComponents()).hasSize(4);
		assertThat(ProductRegistrationDraft.class.getRecordComponents())
				.extracting(java.lang.reflect.RecordComponent::getName)
				.containsExactlyInAnyOrder("name", "brand", "type", "keyIngredients");
	}

	@Test
	void keyIngredients에_레티놀_살리실릭애씨드_아스코르빅애씨드가_있으면_세_태그를_결정적으로_매칭한다() {
		ProductRegistrationDraftGenerator generator = rawText ->
				new ProductRegistrationDraft(null, null, null, "정제수, 레티놀, 살리실릭애씨드, 아스코르빅애씨드");
		ProductRegistrationDraftService service = serviceWith(Optional.of(generator));

		ProductRegistrationDraftResult result = service.createDraft("아무 텍스트");

		assertThat(result.interactionTags())
				.containsExactlyInAnyOrder(InteractionTag.RETINOL, InteractionTag.ACID, InteractionTag.VITAMIN_C);
	}

	@Test
	void 매칭_근거가_없으면_interactionTags는_비어있다() {
		ProductRegistrationDraftGenerator generator = rawText ->
				new ProductRegistrationDraft(null, null, null, "정제수, 글리세린, 나이아신아마이드");
		ProductRegistrationDraftService service = serviceWith(Optional.of(generator));

		ProductRegistrationDraftResult result = service.createDraft("아무 텍스트");

		assertThat(result.interactionTags()).isEmpty();
	}

	@Test
	void keyIngredients가_null이면_interactionTags는_비어있다() {
		ProductRegistrationDraftGenerator generator = rawText ->
				new ProductRegistrationDraft(null, null, null, null);
		ProductRegistrationDraftService service = serviceWith(Optional.of(generator));

		ProductRegistrationDraftResult result = service.createDraft("아무 텍스트");

		assertThat(result.interactionTags()).isEmpty();
	}

	@Test
	void HIGH_CONCENTRATION과_LOW_IRRITATION은_어떤_keyIngredients로도_매칭되지_않는다() {
		// 성분명만으로는 농도/저자극 여부를 판단할 근거가 없다 — 이번 범위에서 의도적으로 보류.
		ProductRegistrationDraftGenerator generator = rawText ->
				new ProductRegistrationDraft(null, null, null,
						"정제수, 레티놀, 살리실릭애씨드, 아스코르빅애씨드, 고농도, 저자극");
		ProductRegistrationDraftService service = serviceWith(Optional.of(generator));

		ProductRegistrationDraftResult result = service.createDraft("아무 텍스트");

		assertThat(result.interactionTags())
				.doesNotContain(InteractionTag.HIGH_CONCENTRATION, InteractionTag.LOW_IRRITATION);
	}

	@Test
	void blank_rawText는_INVALID_REQUEST다() {
		ProductRegistrationDraftService service = serviceWith(Optional.empty());

		assertThatThrownBy(() -> service.createDraft("   "))
				.isInstanceOf(AfterglowException.class)
				.satisfies(e -> assertThat(((AfterglowException) e).getErrorCode())
						.isEqualTo(ErrorCode.INVALID_REQUEST));
	}

	@Test
	void null_rawText는_INVALID_REQUEST다() {
		ProductRegistrationDraftService service = serviceWith(Optional.empty());

		assertThatThrownBy(() -> service.createDraft(null))
				.isInstanceOf(AfterglowException.class)
				.satisfies(e -> assertThat(((AfterglowException) e).getErrorCode())
						.isEqualTo(ErrorCode.INVALID_REQUEST));
	}

	@Test
	void generator_빈이_없으면_AI_REQUEST_FAILED다() {
		ProductRegistrationDraftService service = serviceWith(Optional.empty());

		assertThatThrownBy(() -> service.createDraft("정제수, 글리세린"))
				.isInstanceOf(AfterglowException.class)
				.satisfies(e -> assertThat(((AfterglowException) e).getErrorCode())
						.isEqualTo(ErrorCode.AI_REQUEST_FAILED));
	}

	@Test
	void generator가_AI_REQUEST_FAILED로_실패하면_그대로_전파되고_조용히_숨기지_않는다() {
		ProductRegistrationDraftGenerator failingGenerator = rawText -> {
			throw new AfterglowException(ErrorCode.AI_REQUEST_FAILED);
		};
		ProductRegistrationDraftService service = serviceWith(Optional.of(failingGenerator));

		assertThatThrownBy(() -> service.createDraft("정제수, 글리세린"))
				.isInstanceOf(AfterglowException.class)
				.satisfies(e -> assertThat(((AfterglowException) e).getErrorCode())
						.isEqualTo(ErrorCode.AI_REQUEST_FAILED));
	}

	private ProductRegistrationDraftService serviceWith(Optional<ProductRegistrationDraftGenerator> generator) {
		return new ProductRegistrationDraftService(generator);
	}
}
