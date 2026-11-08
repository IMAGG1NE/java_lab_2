package model;

import java.util.List;
import java.util.Map;

public class ResultQ{
    public final boolean isSuccess;
    public final String message;
    public final List<String> columns;
    public final List<Map<String, Object>> rows;

    public ResultQ(boolean isSuccess, String message, List<String> columns, List<Map<String, Object>> rows){
        this.isSuccess = isSuccess;
        this.message = message;
        this.columns = columns;
        this.rows = rows;
    }
}