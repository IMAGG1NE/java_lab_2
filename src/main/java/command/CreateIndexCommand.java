package command;

import engine.DbEngine;
import model.ResultQ;

public class CreateIndexCommand extends AbstractCommand {
    private final String indexName;
    private final String table;
    private final String column;

    public CreateIndexCommand(String indexName, String table, String column) {
        this.indexName = indexName;
        this.table = table;
        this.column = column;
    }

    @Override
    protected ResultQ run(DbEngine engine) {
        engine.createIndex(table, column);
        return new ResultQ(true, "Индекс " + indexName + " создан", null, null);
    }
}