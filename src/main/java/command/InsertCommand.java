package command;

import engine.DbEngine;
import model.ResultQ;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Команда для вставки новых записей в таблицу.
 * Поддерживает вставку как с явным указанием колонок, так и без него.
 */
public class InsertCommand extends AbstractCommand {
    private final String table;
    private final List<String> columns;
    private final List<List<Object>> rows;

    /**
     * Инициализирует команду вставки.
     *
     * @param table   имя таблицы
     * @param columns список целевых колонок (может быть null, если переданы все значения)
     * @param rows    список строк для вставки (каждая строка — это список значений)
     */
    public InsertCommand(String table, List<String> columns, List<List<Object>> rows) {
        this.table = table;
        this.columns = columns;
        this.rows = rows;
    }

    /**
     * Выполняет вставку данных в таблицу.
     *
     * @param engine движок базы данных
     * @return результат выполнения с количеством добавленных записей или сообщением об ошибке
     */
    @Override
    protected ResultQ run(DbEngine engine) {
        List<String> cols = columns != null ? columns : engine.getColumnNames(table);
        for (List<Object> row : rows) {
            if (row.size() != cols.size()) {
                throw new IllegalArgumentException("Ожидалось значений: " + cols.size()
                        + ", передано: " + row.size());
            }
        }
        int inserted = 0;
        for (List<Object> row : rows) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < cols.size(); i++) {
                values.put(cols.get(i), row.get(i));
            }
            try {
                engine.insert(table, values);
                inserted++;
            } catch (RuntimeException e) {
                return new ResultQ(false, e.getMessage() + " (вставлено до ошибки: " + inserted + ")", null, null);
            }
        }
        return new ResultQ(true, "Вставлено записей: " + inserted, null, null);
    }
}