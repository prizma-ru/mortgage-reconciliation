package com.mortgage.reconciliation.config;

import com.mortgage.reconciliation.context.HeaderContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderInterceptorTest {

    private final HeaderInterceptor interceptor = new HeaderInterceptor();

    @AfterEach
    void clearContext() {
        HeaderContext.clear();
        MDC.clear();
    }

    @Test
    void fillsContextFromHeadersIgnoringCase() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/analyses");
        request.addHeader("Operation-Id", "ANALYZE_CONTRACT");
        request.addHeader("x-call-id", "0192f0e0-0000-7000-8000-000000000001");
        request.addHeader("x-mdm-id", "mdm-1");
        request.addHeader("x-user-session-id", "session-1");
        request.addHeader("X-Segment-Key", "APP-42");

        boolean proceed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(proceed).isTrue();
        assertThat(HeaderContext.getOperationId()).isEqualTo("ANALYZE_CONTRACT");
        assertThat(HeaderContext.getCallId()).isEqualTo("0192f0e0-0000-7000-8000-000000000001");
        assertThat(HeaderContext.getMdmId()).isEqualTo("mdm-1");
        assertThat(HeaderContext.getUserSessionId()).isEqualTo("session-1");
        assertThat(HeaderContext.getSegmentKey()).isEqualTo("APP-42");
        assertThat(MDC.get(HeaderInterceptor.MDC_CALL_ID))
                .isEqualTo("0192f0e0-0000-7000-8000-000000000001");
        assertThat(MDC.get(HeaderInterceptor.MDC_OPERATION_ID)).isEqualTo("ANALYZE_CONTRACT");
    }

    @Test
    void generatesCallIdWhenHeaderIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/analyses");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        String callId = HeaderContext.getCallId();
        assertThat(callId).isNotBlank();
        assertThat(UUID.fromString(callId).version()).isEqualTo(7);
        assertThat(HeaderContext.getOperationId()).isNull();
        assertThat(MDC.get(HeaderInterceptor.MDC_CALL_ID)).isEqualTo(callId);
    }

    @Test
    void clearsContextAndMdcAfterCompletion() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/analyses");
        request.addHeader("Operation-Id", "GET_ANALYSES");
        request.addHeader("x-call-id", "call-1");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        assertThat(HeaderContext.get()).isNull();
        assertThat(MDC.get(HeaderInterceptor.MDC_CALL_ID)).isNull();
        assertThat(MDC.get(HeaderInterceptor.MDC_OPERATION_ID)).isNull();
    }
}
