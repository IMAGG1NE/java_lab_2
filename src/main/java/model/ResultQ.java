package model;

import java.util.List;
import java.util.Map;

/**
 * Класс-контейнер для хранения результата выполнения SQL-запроса.
 * Возвращается командами и используется для вывода результата в консоль.
 */
public class ResultQ {

    /** Флаг успешного выполнения операции. */
    public final boolean isSuccess;

    /** Сообщение об успехе или текст ошибки. */
    public final String message;

    /** Список названий колонок (для SELECT-запросов). */
    public final List<String> columns;

    /** Список извлеченных строк данных (для SELECT-запросов). */
    public final List<Map<String, Object>> rows;

    /**
     * Инициализирует объект результата.
     *
     * @param isSuccess статус выполнения (true - успех, false - ошибка)
     * @param message   информационное сообщение об операции
     * @param columns   список колонок (может быть null)
     * @param rows      выбранные строки (может быть null)
     */
    public ResultQ(boolean isSuccess, String message, List<String> columns, List<Map<String, Object>> rows) {
        this.isSuccess = isSuccess;
        this.message = message;
        this.columns = columns;
        this.rows = rows;
    }
}