package command;

import engine.DbEngine;
import model.ResultQ;

/**
 * Команда, представляющая ошибку синтаксиса SQL.
 * Возвращается парсером, если входная строка не может быть корректно распознана.
 */
public class ErrorCommand implements Command {
    private final String reason;

    /**
     * Инициализирует команду с описанием ошибки.
     *
     * @param reason причина синтаксической ошибки
     */
    public ErrorCommand(String reason) {
        this.reason = reason;
    }

    /**
     * Возвращает результат с ошибкой без обращения к базе данных.
     *
     * @param engine движок базы данных (не используется)
     * @return результат выполнения со статусом ошибки и описанием причины
     */
    @Override
    public ResultQ execute(DbEngine engine) {
        return new ResultQ(false, "Ошибка синтаксиса SQL: " + reason, null, null);
    }
}