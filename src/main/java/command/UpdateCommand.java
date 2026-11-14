package command;

import engine.DbEngine;
import model.Condition;
import model.ResultQ;
import java.util.Map;

/**
 * Команда для обновления существующих записей в таблице.
 * Позволяет изменить значения указанных колонок у строк, удовлетворяющих условию фильтрации.
 */
public class UpdateCommand extends AbstractCommand {
    private final String table;
    private final Map<String, Object> values;
    private final Condition condition;

    /**
     * Инициализирует команду обновления.
     *
     * @param table     имя таблицы, в которой будут обновлены данные
     * @param values    карта новых значений (имя колонки -> новое значение)
     * @param condition условие фильтрации для отбора обновляемых строк (может быть null для обновления всех строк)
     */
    public UpdateCommand(String table, Map<String, Object> values, Condition condition) {
        this.table = table;
        this.values = values;
        this.condition = condition;
    }

    /**
     * Выполняет обновление строк в движке БД.
     *
     * @param engine движок базы данных
     * @return результат выполнения с количеством успешно обновленных записей
     */
    @Override
    protected ResultQ run(DbEngine engine) {
        int count = engine.update(table, values, condition);
        return new ResultQ(true, "Обновлено записей: " + count, null, null);
    }
}