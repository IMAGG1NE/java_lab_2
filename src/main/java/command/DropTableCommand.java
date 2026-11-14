package command;

import engine.DbEngine;
import model.ResultQ;

/**
 * Команда для удаления таблицы и всех её данных.
 */
public class DropTableCommand extends AbstractCommand {
    private final String table;

    /**
     * Инициализирует команду удаления таблицы.
     *
     * @param table имя удаляемой таблицы
     */
    public DropTableCommand(String table) {
        this.table = table;
    }

    /**
     * Выполняет удаление таблицы в движке БД.
     *
     * @param engine движок базы данных
     * @return результат выполнения с сообщением об успехе
     */
    @Override
    protected ResultQ run(DbEngine engine) {
        engine.dropTable(table);
        return new ResultQ(true, "Таблица удалена", null, null);
    }
}