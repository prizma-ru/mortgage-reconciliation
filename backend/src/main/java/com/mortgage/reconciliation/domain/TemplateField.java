package com.mortgage.reconciliation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Поле шаблона: правило извлечения одного значения из документа.
 */
@Entity
@Table(name = "template_field")
public class TemplateField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(name = "field_code", nullable = false, length = 64)
    private String fieldCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "regex_pattern", nullable = false, length = 2000)
    private String regexPattern;

    /**
     * Список якорных фраз через запятую, например {@code "сумма кредита,сумма займа"}.
     */
    @Column(name = "anchor_phrases", length = 1000)
    private String anchorPhrases;

    @Column(name = "normalization_rule", length = 32)
    private String normalizationRule;

    protected TemplateField() {
    }

    public TemplateField(String fieldCode, String title, String regexPattern) {
        this.fieldCode = fieldCode;
        this.title = title;
        this.regexPattern = regexPattern;
    }

    public Long getId() {
        return id;
    }

    /**
     * Родительский шаблон; Hibernate использует это свойство при загрузке поля.
     *
     * @return шаблон документа
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * Проставляет шаблон; вызывается только {@link Template#addField(TemplateField)},
     * чтобы обе стороны связи оставались согласованными.
     *
     * @param template шаблон документа
     */
    public void setTemplate(Template template) {
        this.template = template;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    public String getAnchorPhrases() {
        return anchorPhrases;
    }

    public void setAnchorPhrases(String anchorPhrases) {
        this.anchorPhrases = anchorPhrases;
    }

    public String getNormalizationRule() {
        return normalizationRule;
    }

    public void setNormalizationRule(String normalizationRule) {
        this.normalizationRule = normalizationRule;
    }
}
