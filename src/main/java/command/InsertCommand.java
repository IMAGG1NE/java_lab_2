package command;

import engine.DbEngine;
import model.ResultQ;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InsertCommand extends AbstractCommand {
    private final String table;
    private final List<String> columns; 
    private final List<List<Object>> rows;

    public InsertCommand(String table, List<String> columns, List<List<Object>> rows) {
        this.table = table;
        this.columns = columns;
        this.rows = rows;
    }

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