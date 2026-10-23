package com.mortgage.reconciliation.config;

import com.mortgage.reconciliation.context.HeaderContext;
import com.mortgage.reconciliation.context.HeaderValues;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Складывает сквозные заголовки запроса в {@link HeaderContext} и MDC.
 *
 * <p>{@code HttpServletRequest#getHeader} не чувствителен к регистру, поэтому
 * {@code Operation-Id}/{@code operation-id} и {@code X-Segment-Key}/{@code x-segment-key}
 * читаются одинаково.
 *
 * <p>Контекст заполняется всегда, даже если заголовков нет: {@code x-call-id} генерируется,
 * а остальные остаются {@code null}. Это позволяет сервису работать при вызовах без
 * корпоративных заголовков (внутренние тесты, health-check) и не отклонять их.
 */
@Component
public class HeaderInterceptor implements HandlerInterceptor {

    /** Ключ MDC для сквозного идентификатора запроса. */
    public static final String MDC_CALL_ID = "x-call-id";

    /** Ключ MDC для идентификатора операции. */
    public static final String MDC_OPERATION_ID = "operation-id";

    /**
     * Заполняет контекст и MDC перед вызовом контроллера.
     *
     * @param request  входящий запрос
     * @param response ответ
     * @param handler  выбранный обработчик
     * @return всегда {@code true}: интерсептор ничего не блокирует
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String operationId = request.getHeader("Operation-Id");
        String callId = request.getHeader("x-call-id");

        if (callId == null || callId.isBlank()) {
            callId = HeaderContext.newCallId();
        }

        HeaderValues values = new HeaderValues(
                operationId,
                callId,
                request.getHeader("x-mdm-id"),
                request.getHeader("x-user-session-id"),
                request.getHeader("X-Segment-Key")
        );

        HeaderContext.set(values);
        MDC.put(MDC_CALL_ID, callId);

        if (operationId != null && !operationId.isBlank()) {
            MDC.put(MDC_OPERATION_ID, operationId);
        }

        return true;
    }

    /**
     * Очищает контекст и MDC после завершения запроса, чтобы данные не попали
     * в следующий запрос, обработанный тем же потоком.
     *
     * @param request  входящий запрос
     * @param response ответ
     * @param handler  выбранный обработчик
     * @param ex       исключение, если запрос завершился ошибкой
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                @Nullable Exception ex) {
        HeaderContext.clear();
        MDC.remove(MDC_CALL_ID);
        MDC.remove(MDC_OPERATION_ID);
    }
}
