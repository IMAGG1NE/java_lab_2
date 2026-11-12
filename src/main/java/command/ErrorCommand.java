package command;

import engine.DbEngine;
import model.ResultQ;

public class ErrorCommand implements Command {
    private final String reason;

    public ErrorCommand(String reason) {
        this.reason = reason;
    }

    @Override
    public ResultQ execute(DbEngine engine) {
        return new ResultQ(false, "Ошибка синтаксиса SQL: " + reason, null, null);
    }
}