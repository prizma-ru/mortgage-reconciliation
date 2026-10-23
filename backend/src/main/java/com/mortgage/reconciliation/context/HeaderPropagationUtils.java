package com.mortgage.reconciliation.context;

import org.springframework.http.HttpHeaders;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Передаёт сквозные заголовки в исходящие запросы (например, при вызове внешнего API).
 *
 * <p>Заголовки берутся из {@link HeaderContext}; отсутствующие (опциональные) не добавляются,
 * чтобы не отправлять пустые значения. {@code Authorization}, {@code Cookie} и
 * {@code device-fingerprint} сюда намеренно не попадают — контекст их не хранит.
 */
public final class HeaderPropagationUtils {

    /** Имя заголовка идентификатора операции. */
    public static final String OPERATION_ID = "Operation-Id";

    /** Имя заголовка сквозного идентификатора запроса. */
    public static final String CALL_ID = "x-call-id";

    /** Имя заголовка идентификатора клиента. */
    public static final String MDM_ID = "x-mdm-id";

    /** Имя заголовка идентификатора сессии. */
    public static final String USER_SESSION_ID = "x-user-session-id";

    /** Имя заголовка ключа маршрутизации. */
    public static final String SEGMENT_KEY = "X-Segment-Key";

    private HeaderPropagationUtils() {
    }

    /**
     * Собирает заголовки текущего запроса для исходящего вызова.
     *
     * @return карта «имя заголовка → значение» без пустых значений
     */
    public static Map<String, String> collectHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();

        putIfPresent(headers, OPERATION_ID, HeaderContext.getOperationId());
        putIfPresent(headers, CALL_ID, HeaderContext.getCallId());
        putIfPresent(headers, MDM_ID, HeaderContext.getMdmId());
        putIfPresent(headers, USER_SESSION_ID, HeaderContext.getUserSessionId());
        putIfPresent(headers, SEGMENT_KEY, HeaderContext.getSegmentKey());

        return headers;
    }

    /**
     * Добавляет заголовки текущего запроса в {@link HttpHeaders} перед отправкой.
     *
     * @param target заголовки исходящего запроса; не должен быть {@code null}
     */
    public static void apply(HttpHeaders target) {
        collectHeaders().forEach(target::set);
    }

    private static void putIfPresent(Map<String, String> headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.put(name, value);
        }
    }
}
