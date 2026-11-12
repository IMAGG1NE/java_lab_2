package command;

public class SqlSyntaxException extends RuntimeException {

    public SqlSyntaxException(String message) {
        super(message);
    }
}