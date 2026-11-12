package command;

import engine.DbEngine;
import model.Condition;
import model.ResultQ;

public class DeleteCommand extends AbstractCommand {
    private final String table;
    private final Condition condition;

    public DeleteCommand(String table, Condition condition) {
        this.table = table;
        this.condition = condition;
    }

    @Override
    protected ResultQ run(DbEngine engine) {
        int count = engine.delete(table, condition);
        return new ResultQ(true, "Удалено записей: " + count, null, null);
    }
}