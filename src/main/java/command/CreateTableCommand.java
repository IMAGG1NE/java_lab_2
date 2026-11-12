package command;

import engine.DbEngine;
import model.DataType;
import model.ResultQ;
import java.util.Map;

public class CreateTableCommand extends AbstractCommand {
    private final String table;
    private final Map<String, DataType> columns;

    public CreateTableCommand(String table, Map<String, DataType> columns) {
        this.table = table;
        this.columns = columns;
    }

    @Override
    protected ResultQ run(DbEngine engine) {
        engine.createTable(table, columns);
        return new ResultQ(true, "Таблица создана", null, null);
    }
}
