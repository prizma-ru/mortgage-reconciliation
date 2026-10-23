package com.mortgage.reconciliation.dto;

import com.mortgage.reconciliation.domain.AnalysisFieldResult;
import com.mortgage.reconciliation.domain.AnalysisRun;
import com.mortgage.reconciliation.domain.AnalysisRunStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisRunResponseTest {

    @Test
    void mapsRunAndFieldResults() {
        AnalysisRun run = new AnalysisRun("DDU", "contract.docx", AnalysisRunStatus.NEEDS_REVIEW);
        run.addResult(new AnalysisFieldResult(new ComparisonResult(
                "loan_amount", "Сумма кредита", "1000000", "1200000", "paragraph 1",
                ComparisonStatus.ERROR, "Значения различаются")));
        run.addResult(new AnalysisFieldResult(new ComparisonResult(
                "interest_rate", "Процентная ставка", null, "12.5", "paragraph 2",
                ComparisonStatus.WARN, "Поле есть в договоре, но отсутствует в заявке")));

        AnalysisRunResponse response = AnalysisRunResponse.from(run);

        assertThat(response.fileName()).isEqualTo("contract.docx");
        assertThat(response.docType()).isEqualTo("DDU");
        assertThat(response.status()).isEqualTo("NEEDS_REVIEW");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.results()).hasSize(2);

        FieldResultResponse first = response.results().get(0);
        assertThat(first.fieldCode()).isEqualTo("loan_amount");
        assertThat(first.status()).isEqualTo("ERROR");
        assertThat(first.location()).isEqualTo("paragraph 1");
        assertThat(first.appValue()).isEqualTo("1000000");
        assertThat(first.contractValue()).isEqualTo("1200000");

        assertThat(response.results().get(1).appValue()).isNull();
    }

    @Test
    void returnsEmptyResultsForRunWithoutFields() {
        AnalysisRun run = new AnalysisRun("DDU", "empty.docx", AnalysisRunStatus.DONE);

        AnalysisRunResponse response = AnalysisRunResponse.from(run);

        assertThat(response.results()).isEmpty();
        assertThat(response.status()).isEqualTo("DONE");
    }

    @Test
    void keepsResultsOrder() {
        AnalysisRun run = new AnalysisRun("DDU", "contract.docx", AnalysisRunStatus.DONE);

        List.of("loan_amount", "interest_rate", "borrower_fio").forEach(code ->
                run.addResult(new AnalysisFieldResult(new ComparisonResult(
                        code, code, "v", "v", "paragraph 1", ComparisonStatus.OK, "ok"))));

        assertThat(AnalysisRunResponse.from(run).results())
                .extracting(FieldResultResponse::fieldCode)
                .containsExactly("loan_amount", "interest_rate", "borrower_fio");
    }
}
