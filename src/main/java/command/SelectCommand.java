package command;

import engine.DbEngine;
import model.Condition;
import model.ResultQ;
import java.util.List;
import java.util.Map;

public class SelectCommand extends AbstractCommand {
    private final String table;
    private final List<String> columns;
    private final Condition condition;

    public SelectCommand(String table, List<String> columns, Condition condition) {
        this.table = table;
        this.columns = columns;
        this.condition = condition;
    }

    @Override
    protected ResultQ run(DbEngine engine) {
        List<Map<String, Object>> rows = engine.select(table, columns, condition);
        boolean all = columns.size() == 1 && "*".equals(columns.get(0));
        List<String> resultColumns = all ? engine.getColumnNames(table) : columns;
        return new ResultQ(true, "ОК", resultColumns, rows);
    }
}