package command;

import engine.DbEngine;
import model.Condition;
import model.ResultQ;
import java.util.List;
import java.util.Map;


/**
 * Команда для выборки данных из таблицы.
 * Поддерживает проекцию (выбор конкретных колонок) и фильтрацию по условию.
 */
public class SelectCommand extends AbstractCommand {
    private final String table;
    private final List<String> columns;
    private final Condition condition;

    /**
     * Инициализирует команду выборки.
     *
     * @param table     имя таблицы, из которой извлекаются данные
     * @param columns   список запрашиваемых колонок (или список с одним элементом "*", если нужны все)
     * @param condition условие фильтрации (может быть null, если выборка без фильтра)
     */
    public SelectCommand(String table, List<String> columns, Condition condition) {
        this.table = table;
        this.columns = columns;
        this.condition = condition;
    }

    /**
     * Выполняет выборку строк в движке БД.
     *
     * @param engine движок базы данных
     * @return результат выполнения, содержащий список имен колонок и извлеченные строки данных
     */
    @Override
    protected ResultQ run(DbEngine engine) {
        List<Map<String, Object>> rows = engine.select(table, columns, condition);
        boolean all = columns.size() == 1 && "*".equals(columns.get(0));
        List<String> resultColumns = all ? engine.getColumnNames(table) : columns;
        return new ResultQ(true, "ОК", resultColumns, rows);
    }
}