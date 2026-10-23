package com.mortgage.reconciliation.context;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Контекст сквозных заголовков текущего запроса.
 *
 * <p>Значения хранятся в {@link ThreadLocal}: Spring MVC обрабатывает запрос в одном потоке,
 * а после завершения {@code HeaderInterceptor} очищает контекст, чтобы не утекала память
 * и не смешивались данные соседних запросов в пуле потоков Tomcat.
 *
 * <p>Если запрос пришёл без заголовков (например, внутренний вызов или тест), методы возвращают
 * {@code null} и не бросают исключений — сервис продолжает работать с доступными данными.
 */
public final class HeaderContext {

    private static final ThreadLocal<HeaderValues> CURRENT = new ThreadLocal<>();

    private HeaderContext() {
    }

    /**
     * Возвращает заголовки текущего запроса.
     *
     * @return значения заголовков или {@code null}, если контекст не установлен
     */
    public static HeaderValues get() {
        return CURRENT.get();
    }

    /**
     * Сохраняет заголовки текущего запроса.
     *
     * @param values значения заголовков; {@code null} очищает контекст
     */
    public static void set(HeaderValues values) {
        if (values == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(values);
        }
    }

    /**
     * Идентификатор операции из текущего запроса.
     *
     * @return значение {@code Operation-Id} или {@code null}
     */
    public static String getOperationId() {
        HeaderValues values = CURRENT.get();
        return values == null ? null : values.operationId();
    }

    /**
     * Сквозной идентификатор запроса из текущего запроса.
     *
     * @return значение {@code x-call-id} или {@code null}
     */
    public static String getCallId() {
        HeaderValues values = CURRENT.get();
        return values == null ? null : values.callId();
    }

    /**
     * Идентификатор клиента из текущего запроса.
     *
     * @return значение {@code x-mdm-id} или {@code null}
     */
    public static String getMdmId() {
        HeaderValues values = CURRENT.get();
        return values == null ? null : values.mdmId();
    }

    /**
     * Идентификатор сессии пользователя из текущего запроса.
     *
     * @return значение {@code x-user-session-id} или {@code null}
     */
    public static String getUserSessionId() {
        HeaderValues values = CURRENT.get();
        return values == null ? null : values.userSessionId();
    }

    /**
     * Ключ маршрутизации из текущего запроса.
     *
     * @return значение {@code X-Segment-Key} или {@code null}
     */
    public static String getSegmentKey() {
        HeaderValues values = CURRENT.get();
        return values == null ? null : values.segmentKey();
    }

    /**
     * Очищает контекст потока. Вызывается после завершения запроса.
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Генерирует {@code x-call-id} по RFC 9562 (UUID v7): первые 48 бит — миллисекунды,
     * версия 7 и случайный хвост.
     *
     * <p>UUID v7 выбран потому, что он сортируем по времени: по идентификатору видно,
     * когда начался запрос, и такие значения удобно индексировать в логах.
     *
     * @return новый UUID версии 7
     */
    public static String newCallId() {
        long timestamp = System.currentTimeMillis();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 48 бит времени, 4 бита версии (0111), 12 бит случайности — старшая половина.
        long mostSignificant = (timestamp << 16)
                | (0x7L << 12)
                | (random.nextLong() & 0xFFFL);

        // Два старших бита варианта (10) и 62 бита случайности — младшая половина.
        long leastSignificant = (0b10L << 62) | (random.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL);

        return new UUID(mostSignificant, leastSignificant).toString();
    }
}
