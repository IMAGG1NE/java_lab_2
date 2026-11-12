package command;

import engine.DbEngine;
import model.Condition;
import model.ResultQ;
import java.util.Map;

public class UpdateCommand extends AbstractCommand {
    private final String table;
    private final Map<String, Object> values;
    private final Condition condition;

    public UpdateCommand(String table, Map<String, Object> values, Condition condition) {
        this.table = table;
        this.values = values;
        this.condition = condition;
    }

    @Override
    protected ResultQ run(DbEngine engine) {
        int count = engine.update(table, values, condition);
        return new ResultQ(true, "Обновлено записей: " + count, null, null);
    }
}