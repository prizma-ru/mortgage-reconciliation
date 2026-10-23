package com.mortgage.reconciliation.context;

/**
 * Сквозные заголовки запроса, доступные в течение обработки запроса.
 *
 * @param operationId    идентификатор операции, например {@code ANALYZE_CONTRACT}
 * @param callId         сквозной идентификатор запроса (UUID v7)
 * @param mdmId          идентификатор клиента (может отсутствовать)
 * @param userSessionId  идентификатор сессии пользователя (может отсутствовать)
 * @param segmentKey     ключ маршрутизации, например номер заявки (может отсутствовать)
 */
public record HeaderValues(
        String operationId,
        String callId,
        String mdmId,
        String userSessionId,
        String segmentKey
) {
}
