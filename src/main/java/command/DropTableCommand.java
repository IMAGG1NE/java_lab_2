package command;

import engine.DbEngine;
import model.ResultQ;

public class DropTableCommand extends AbstractCommand {
    private final String table;

    public DropTableCommand(String table) {
        this.table = table;
    }

    @Override
    protected ResultQ run(DbEngine engine) {
        engine.dropTable(table);
        return new ResultQ(true, "Таблица удалена", null, null);
    }
}