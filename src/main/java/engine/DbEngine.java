package engine;

import model.Condition;
import java.util.List;
import java.util.Map;

public interface DbEngine {
    void insert(String table_name, Map<String, Object> vals);
    int update(String table_name, Map<String, Object> vals, Condition cond);
    int delete(String table_name, Condition cond);
    List<Map<String, Object>> select(String table_name, List<String> columns, Condition cond);

    void createTable(String table_name, List<String> columns);
    void dropTable(String table_name);
    void createIndex(String table_name, String column_name);
}
