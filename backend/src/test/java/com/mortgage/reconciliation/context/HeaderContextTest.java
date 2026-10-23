package com.mortgage.reconciliation.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HeaderContextTest {

    @AfterEach
    void clearContext() {
        HeaderContext.clear();
    }

    @Test
    void storesAndReturnsHeaderValues() {
        HeaderContext.set(new HeaderValues("ANALYZE_CONTRACT", "0192f0e0-0000-7000-8000-000000000001",
                "mdm-1", "session-1", "APP-42"));

        assertThat(HeaderContext.getOperationId()).isEqualTo("ANALYZE_CONTRACT");
        assertThat(HeaderContext.getCallId()).isEqualTo("0192f0e0-0000-7000-8000-000000000001");
        assertThat(HeaderContext.getMdmId()).isEqualTo("mdm-1");
        assertThat(HeaderContext.getUserSessionId()).isEqualTo("session-1");
        assertThat(HeaderContext.getSegmentKey()).isEqualTo("APP-42");
    }

    @Test
    void returnsNullWhenContextIsEmpty() {
        assertThat(HeaderContext.get()).isNull();
        assertThat(HeaderContext.getCallId()).isNull();
        assertThat(HeaderContext.getOperationId()).isNull();
        assertThat(HeaderContext.getMdmId()).isNull();
        assertThat(HeaderContext.getUserSessionId()).isNull();
        assertThat(HeaderContext.getSegmentKey()).isNull();
    }

    @Test
    void clearRemovesValues() {
        HeaderContext.set(new HeaderValues("OP", "call", null, null, null));
        HeaderContext.clear();

        assertThat(HeaderContext.get()).isNull();
        assertThat(HeaderContext.getCallId()).isNull();
    }

    @Test
    void generatesUuidVersion7WithEmbeddedTimestamp() {
        String callId = HeaderContext.newCallId();
        UUID uuid = UUID.fromString(callId);

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);

        // В UUID v7 старшие 48 бит — миллисекунды создания, по ним запросы сортируются по времени.
        long timestamp = uuid.getMostSignificantBits() >>> 16;
        assertThat(timestamp).isCloseTo(System.currentTimeMillis(), within(2_000L));
    }

    @Test
    void generatesUniqueCallIds() {
        assertThat(HeaderContext.newCallId()).isNotEqualTo(HeaderContext.newCallId());
    }
}
