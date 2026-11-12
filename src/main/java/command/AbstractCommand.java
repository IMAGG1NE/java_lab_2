package command;

import engine.DbEngine;
import model.ResultQ;

public abstract class AbstractCommand implements Command {

    @Override
    public final ResultQ execute(DbEngine engine) {
        try {
            return run(engine);
        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            return new ResultQ(false, msg, null, null);
        }
    }

    protected abstract ResultQ run(DbEngine engine);
}