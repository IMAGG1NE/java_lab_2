package command;

import engine.DbEngine;
import model.ResultQ;

/**
 * Команда для создания индекса по указанной колонке таблицы.
 */
public class CreateIndexCommand extends AbstractCommand {
    private final String indexName;
    private final String table;
    private final String column;

    /**
     * Инициализирует команду создания индекса.
     *
     * @param indexName имя создаваемого индекса
     * @param table     имя таблицы
     * @param column    имя колонки для индексации
     */
    public CreateIndexCommand(String indexName, String table, String column) {
        this.indexName = indexName;
        this.table = table;
        this.column = column;
    }

    /**
     * Выполняет создание индекса в движке БД.
     *
     * @param engine движок базы данных
     * @return результат выполнения с сообщением об успехе
     */
    @Override
    protected ResultQ run(DbEngine engine) {
        engine.createIndex(table, column);
        return new ResultQ(true, "Индекс " + indexName + " создан", null, null);
    }
}