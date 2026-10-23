package com.mortgage.reconciliation.service;

import com.mortgage.reconciliation.domain.Template;
import com.mortgage.reconciliation.domain.TemplateField;
import com.mortgage.reconciliation.dto.ApplicationData;
import com.mortgage.reconciliation.dto.ComparisonResult;
import com.mortgage.reconciliation.dto.ComparisonStatus;
import com.mortgage.reconciliation.dto.ExtractedField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComparatorServiceTest {

    private final ComparatorService service = new ComparatorService(new NormalizationService());

    @Test
    void marksEqualValuesAsOk() {
        Template template = template();
        ApplicationData appData = new ApplicationData(Map.of(
                "loan_amount", "1000000.5",
                "interest_rate", "6.5",
                "borrower_fio", "иванов иван иванович"));

        List<ComparisonResult> results = service.compare(appData, List.of(
                field("loan_amount", "1 000 000,50 руб."),
                field("interest_rate", "6,5 %"),
                field("borrower_fio", "Иванов Иван Иванович")
        ), template);

        assertThat(results).extracting(ComparisonResult::status)
                .containsOnly(ComparisonStatus.OK);
        assertThat(results.get(0).appValue()).isEqualTo("1000000.5");
        assertThat(results.get(0).contractValue()).isEqualTo("1000000.5");
    }

    @Test
    void reportsMissingWhenContractHasNoValue() {
        List<ComparisonResult> results = service.compare(
                new ApplicationData(Map.of("loan_amount", "1000000")),
                List.of(),
                template());

        ComparisonResult loanAmount = results.get(0);
        assertThat(loanAmount.status()).isEqualTo(ComparisonStatus.MISSING);
        assertThat(loanAmount.appValue()).isEqualTo("1000000");
        assertThat(loanAmount.contractValue()).isNull();
    }

    @Test
    void reportsWarnWhenApplicationHasNoValue() {
        List<ComparisonResult> results = service.compare(
                new ApplicationData(Map.of("loan_amount", "1000000")),
                List.of(field("interest_rate", "6,5 %")),
                template());

        ComparisonResult interestRate = results.get(1);
        assertThat(interestRate.status()).isEqualTo(ComparisonStatus.WARN);
        assertThat(interestRate.appValue()).isNull();
        assertThat(interestRate.contractValue()).isEqualTo("6.5");
    }

    @Test
    void reportsErrorWhenValuesDiffer() {
        List<ComparisonResult> results = service.compare(
                new ApplicationData(Map.of("loan_amount", "900000")),
                List.of(field("loan_amount", "1 000 000 руб.")),
                template());

        ComparisonResult loanAmount = results.get(0);
        assertThat(loanAmount.status()).isEqualTo(ComparisonStatus.ERROR);
        assertThat(loanAmount.message()).contains("900000").contains("1000000");
    }

    @Test
    void reportsManualWhenContractHasSeveralDifferentValues() {
        List<ComparisonResult> results = service.compare(
                new ApplicationData(Map.of("loan_amount", "1000000")),
                List.of(field("loan_amount", "1 000 000 руб."), field("loan_amount", "1 200 000 руб.")),
                template());

        ComparisonResult loanAmount = results.get(0);
        assertThat(loanAmount.status()).isEqualTo(ComparisonStatus.MANUAL);
        assertThat(loanAmount.contractValue()).isEqualTo("1000000 | 1200000");
    }

    @Test
    void reportsManualWhenValueCannotBeNormalized() {
        List<ComparisonResult> results = service.compare(
                new ApplicationData(Map.of("loan_amount", "1000000")),
                List.of(field("loan_amount", "сумма не указана")),
                template());

        assertThat(results.get(0).status()).isEqualTo(ComparisonStatus.MANUAL);
    }

    @Test
    void comparesFioIgnoringCaseAndSpaces() {
        List<ComparisonResult> results = service.compare(
                new ApplicationData(Map.of("borrower_fio", "  иванов   иван   иваныч ")),
                List.of(field("borrower_fio", "Иванов Иван Иванович")),
                template());

        assertThat(results.get(2).status()).isEqualTo(ComparisonStatus.OK);
    }

    @Test
    void reportsErrorForDifferentPeople() {
        List<ComparisonResult> results = service.compare(
                new ApplicationData(Map.of("borrower_fio", "Петров Пётр Петрович")),
                List.of(field("borrower_fio", "Иванов Иван Иванович")),
                template());

        assertThat(results.get(2).status()).isEqualTo(ComparisonStatus.ERROR);
    }

    private Template template() {
        Template template = new Template("DDU", "Договор долевого участия");

        TemplateField loanAmount = new TemplateField("loan_amount", "Сумма кредита", "ignored");
        loanAmount.setNormalizationRule("MONEY");

        TemplateField interestRate = new TemplateField("interest_rate", "Процентная ставка", "ignored");
        interestRate.setNormalizationRule("PERCENT");

        TemplateField borrowerFio = new TemplateField("borrower_fio", "ФИО заемщика", "ignored");
        borrowerFio.setNormalizationRule("FIO");

        template.addField(loanAmount);
        template.addField(interestRate);
        template.addField(borrowerFio);

        return template;
    }

    private ExtractedField field(String fieldCode, String rawValue) {
        return new ExtractedField(fieldCode, rawValue, "paragraph 1", 0.9);
    }
}
