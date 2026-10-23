package com.mortgage.reconciliation.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderPropagationUtilsTest {

    @AfterEach
    void clearContext() {
        HeaderContext.clear();
    }

    @Test
    void collectsAllPresentHeaders() {
        HeaderContext.set(new HeaderValues("ANALYZE_CONTRACT", "call-1", "mdm-1", "session-1", "APP-42"));

        Map<String, String> headers = HeaderPropagationUtils.collectHeaders();

        assertThat(headers)
                .containsEntry("Operation-Id", "ANALYZE_CONTRACT")
                .containsEntry("x-call-id", "call-1")
                .containsEntry("x-mdm-id", "mdm-1")
                .containsEntry("x-user-session-id", "session-1")
                .containsEntry("X-Segment-Key", "APP-42");
    }

    @Test
    void skipsMissingOptionalHeaders() {
        HeaderContext.set(new HeaderValues(null, "call-1", null, null, null));

        Map<String, String> headers = HeaderPropagationUtils.collectHeaders();

        assertThat(headers).containsOnlyKeys("x-call-id");
    }

    @Test
    void appliesHeadersToOutgoingRequest() {
        HeaderContext.set(new HeaderValues("OP", "call-1", "mdm-1", null, null));
        HttpHeaders target = new HttpHeaders();

        HeaderPropagationUtils.apply(target);

        assertThat(target.getFirst("Operation-Id")).isEqualTo("OP");
        assertThat(target.getFirst("x-call-id")).isEqualTo("call-1");
        assertThat(target.getFirst("x-mdm-id")).isEqualTo("mdm-1");
        assertThat(target.getFirst("X-Segment-Key")).isNull();
    }

    @Test
    void returnsEmptyMapWithoutContext() {
        assertThat(HeaderPropagationUtils.collectHeaders()).isEmpty();
    }
}
