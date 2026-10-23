package com.mortgage.reconciliation.service;

import com.mortgage.reconciliation.domain.Template;
import com.mortgage.reconciliation.domain.TemplateField;
import com.mortgage.reconciliation.dto.DocBlock;
import com.mortgage.reconciliation.dto.ExtractedField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegexExtractorServiceTest {

    private static final String MONEY_PATTERN = "(?<value>\\d[\\d\\s]*(?:[.,]\\d{1,2})?)\\s*(?:руб\\.?|₽|RUB)";
    private static final String PERCENT_PATTERN = "(?<value>\\d{1,2}(?:[.,]\\d{1,4})?)\\s*%";

    private final RegexExtractorService service = new RegexExtractorService();

    @Test
    void extractsValueNextToAnchor() {
        Template template = template(
                field("loan_amount", "Сумма кредита", MONEY_PATTERN, "сумма кредита,сумма займа"));

        List<DocBlock> blocks = List.of(
                DocBlock.paragraph("Сумма кредита: 1 000 000 руб. Срок — 20 лет."));

        List<ExtractedField> result = service.extract(blocks, template);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fieldCode()).isEqualTo("loan_amount");
        assertThat(result.get(0).rawValue()).isEqualTo("1 000 000");
        assertThat(result.get(0).location()).isEqualTo("paragraph 1");
        assertThat(result.get(0).confidence()).isEqualTo(0.9);
    }

    @Test
    void fallsBackToWholeBlockWhenAnchorIsMissing() {
        Template template = template(
                field("interest_rate", "Процентная ставка", PERCENT_PATTERN, "процентная ставка"));

        List<DocBlock> blocks = List.of(DocBlock.paragraph("Ставка 12,5 % годовых на весь срок."));

        List<ExtractedField> result = service.extract(blocks, template);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rawValue()).isEqualTo("12,5");
        assertThat(result.get(0).confidence()).isEqualTo(0.4);
    }

    @Test
    void returnsEveryDistinctValueOfSameField() {
        Template template = template(
                field("loan_amount", "Сумма кредита", MONEY_PATTERN, "сумма кредита"));

        List<DocBlock> blocks = List.of(
                DocBlock.paragraph("Сумма кредита 1 000 000 руб."),
                DocBlock.paragraph("Сумма кредита 1 200 000 руб."));

        List<ExtractedField> result = service.extract(blocks, template);

        assertThat(result).extracting(ExtractedField::rawValue)
                .containsExactly("1 000 000", "1 200 000");
        assertThat(result).extracting(ExtractedField::fieldCode)
                .containsOnly("loan_amount");
    }

    @Test
    void collapsesIdenticalValuesKeepingHighestConfidence() {
        Template template = template(
                field("loan_amount", "Сумма кредита", MONEY_PATTERN, "сумма кредита"));

        List<DocBlock> blocks = List.of(
                DocBlock.paragraph("Сумма кредита 1 000 000 руб."),
                DocBlock.paragraph("Итого 1 000 000 руб."));

        List<ExtractedField> result = service.extract(blocks, template);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rawValue()).isEqualTo("1 000 000");
        assertThat(result.get(0).confidence()).isEqualTo(0.9);
    }

    @Test
    void reportsTableCoordinatesAsLocation() {
        Template template = template(
                field("loan_amount", "Сумма кредита", MONEY_PATTERN, null));

        List<DocBlock> blocks = List.of(
                DocBlock.tableCell(0, 1, 2, "1 000 000 руб."));

        List<ExtractedField> result = service.extract(blocks, template);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).location()).isEqualTo("table 0, row 1, cell 2");
        assertThat(result.get(0).confidence()).isEqualTo(0.4);
    }

    @Test
    void skipsFieldWithInvalidRegex() {
        Template template = template(
                field("broken", "Сломанное поле", "([а-я", null));

        List<ExtractedField> result = service.extract(List.of(DocBlock.paragraph("любой текст")), template);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyListForEmptyDocument() {
        Template template = template(
                field("loan_amount", "Сумма кредита", MONEY_PATTERN, "сумма кредита"));

        assertThat(service.extract(List.of(), template)).isEmpty();
    }

    private Template template(TemplateField... fields) {
        Template template = new Template("DDU", "Договор долевого участия");

        for (TemplateField field : fields) {
            template.addField(field);
        }

        return template;
    }

    private TemplateField field(String code, String title, String regex, String anchors) {
        TemplateField field = new TemplateField(code, title, regex);
        field.setAnchorPhrases(anchors);
        return field;
    }
}
