package com.mortgage.reconciliation.exception;

/**
 * Ошибка разбора JSON-заявки АЗС.
 *
 * <p>Сообщение предназначено для пользователя API: оно объясняет, что именно не так
 * с заявкой (пустая строка, не JSON, неверный тип поля), и не содержит внутренних деталей
 * парсера.
 */
public class ApplicationParsingException extends RuntimeException {

    /**
     * @param message понятное описание проблемы
     */
    public ApplicationParsingException(String message) {
        super(message);
    }

    /**
     * @param message понятное описание проблемы
     * @param cause   исходная ошибка Jackson
     */
    public ApplicationParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
