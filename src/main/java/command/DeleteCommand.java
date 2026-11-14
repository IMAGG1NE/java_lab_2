package command;

import engine.DbEngine;
import model.Condition;
import model.ResultQ;

/**
 * Команда для удаления записей из таблицы.
 */
public class DeleteCommand extends AbstractCommand {
    private final String table;
    private final Condition condition;

    /**
     * Инициализирует команду удаления.
     *
     * @param table     имя таблицы
     * @param condition условие фильтрации удаляемых записей
     */
    public DeleteCommand(String table, Condition condition) {
        this.table = table;
        this.condition = condition;
    }

    /**
     * Выполняет удаление строк в движке БД.
     *
     * @param engine движок базы данных
     * @return результат выполнения с количеством удаленных записей
     */
    @Override
    protected ResultQ run(DbEngine engine) {
        int count = engine.delete(table, condition);
        return new ResultQ(true, "Удалено записей: " + count, null, null);
    }
}