package com.afterglow.story.domain;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "story")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Story extends BaseEntity {

	@Column(nullable = false)
	private Long accountId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
		name = "story_symptom_tag",
		joinColumns = @JoinColumn(name = "story_id")
	)
	@Enumerated(EnumType.STRING)
	@Column(name = "symptom_tag", nullable = false)
	private Set<SymptomTag> symptomTags = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
		name = "story_situation_tag",
		joinColumns = @JoinColumn(name = "story_id")
	)
	@Enumerated(EnumType.STRING)
	@Column(name = "situation_tag")
	private Set<SituationTag> situationTags = new LinkedHashSet<>();

	@Enumerated(EnumType.STRING)
	private LifeStage lifeStage;

	@Column(nullable = false)
	private boolean lifeStagePublic;

	@Column(nullable = false)
	private int likeCount;

	@Column(nullable = false)
	private boolean hidden;

	private Story(
		Long accountId,
		String title,
		String content,
		Set<SymptomTag> symptomTags,
		Set<SituationTag> situationTags,
		LifeStage lifeStage,
		boolean lifeStagePublic) {

		this.accountId = accountId;
		this.title = title;
		this.content = content;
		this.symptomTags = new LinkedHashSet<>(symptomTags);
		this.situationTags = new LinkedHashSet<>(situationTags);
		this.lifeStage = lifeStage;
		this.lifeStagePublic = lifeStagePublic;
		this.likeCount = 0;
		this.hidden = false;
	}

	public static Story create(
		Long accountId,
		String title,
		String content,
		Set<SymptomTag> symptomTags,
		Set<SituationTag> situationTags,
		LifeStage lifeStage,
		boolean lifeStagePublic) {

		return new Story(
			accountId,
			title,
			content,
			symptomTags,
			situationTags,
			lifeStage,
			lifeStagePublic
		);
	}
}
