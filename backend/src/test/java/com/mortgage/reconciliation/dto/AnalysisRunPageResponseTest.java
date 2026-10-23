package com.mortgage.reconciliation.dto;

import com.mortgage.reconciliation.domain.AnalysisRun;
import com.mortgage.reconciliation.domain.AnalysisRunStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisRunPageResponseTest {

    @Test
    void mapsPageWithSummaryItems() {
        AnalysisRun first = new AnalysisRun("DDU", "contract.docx", AnalysisRunStatus.NEEDS_REVIEW);
        AnalysisRun second = new AnalysisRun("DKP", "purchase.docx", AnalysisRunStatus.DONE);

        AnalysisRunPageResponse response = AnalysisRunPageResponse.from(
                new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 42));

        assertThat(response.items()).hasSize(2);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(42);
        assertThat(response.totalPages()).isEqualTo(3);

        AnalysisRunSummaryResponse item = response.items().get(0);
        assertThat(item.fileName()).isEqualTo("contract.docx");
        assertThat(item.docType()).isEqualTo("DDU");
        assertThat(item.status()).isEqualTo("NEEDS_REVIEW");
        assertThat(item.createdAt()).isNotNull();
        assertThat(response.items().get(1).status()).isEqualTo("DONE");
    }

    @Test
    void mapsEmptyPage() {
        AnalysisRunPageResponse response = AnalysisRunPageResponse.from(
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(response.items()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }
}
