package command;
import engine.DbEngine;
import model.ResultQ;

public interface Command {
    ResultQ execute(DbEngine engine);
}
