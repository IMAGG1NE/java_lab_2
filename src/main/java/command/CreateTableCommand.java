package command;

import engine.DbEngine;
import model.DataType;
import model.ResultQ;
import java.util.Map;

/**
 * Команда для создания новой таблицы.
 */
public class CreateTableCommand extends AbstractCommand {
    private final String table;
    private final Map<String, DataType> columns;

    /**
     * Инициализирует команду создания таблицы.
     *
     * @param table   имя создаваемой таблицы
     * @param columns карта колонок с их типами данных
     */
    public CreateTableCommand(String table, Map<String, DataType> columns) {
        this.table = table;
        this.columns = columns;
    }

    /**
     * Выполняет создание таблицы в движке БД.
     *
     * @param engine движок базы данных
     * @return результат выполнения с сообщением об успехе
     */
    @Override
    protected ResultQ run(DbEngine engine) {
        engine.createTable(table, columns);
        return new ResultQ(true, "Таблица создана", null, null);
    }
}