package command;

import engine.DbEngine;
import model.ResultQ;

/**
 * Базовый абстрактный класс для всех SQL-команд.
 * Обеспечивает перехват исключений при выполнении.
 */
public abstract class AbstractCommand implements Command {

    /**
     * Выполняет команду и перехватывает ошибки времени выполнения.
     *
     * @param engine движок базы данных
     * @return объект ResultQ с результатами или сообщением об ошибке
     */
    @Override
    public final ResultQ execute(DbEngine engine) {
        try {
            return run(engine);
        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            return new ResultQ(false, msg, null, null);
        }
    }

    /**
     * Специфичная логика команды, которую должны реализовать классы-наследники.
     *
     * @param engine движок базы данных
     * @return результат выполнения операции
     */
    protected abstract ResultQ run(DbEngine engine);
}