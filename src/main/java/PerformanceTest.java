import engine.DbEngine;
import engine.FileDbEngine;
import model.Condition;
import model.DataType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PerformanceTest {
    public static void main(String[] args) {
        DbEngine engine = new FileDbEngine();
        String tableName = "perf_test_table";
        try { engine.dropTable(tableName); } catch (Exception ignored) {}

        Map<String, DataType> schema = new LinkedHashMap<>();
        schema.put("id", DataType.INT);
        schema.put("value", DataType.INT);
        engine.createTable(tableName, schema);
        System.out.println("Генерация и вставка 50 000 записей");
        for (int i = 0; i < 50000; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", i);
            row.put("value", i % 1000);
            engine.insert(tableName, row);

        }
        System.out.println("Вставка завершена.\n");
        Condition cond = new Condition("value", "=", 500);

        long startWithoutIndex = System.currentTimeMillis();
        List<Map<String, Object>> result1 = engine.select(tableName, List.of("*"), cond);
        long timeWithoutIndex = System.currentTimeMillis() - startWithoutIndex;
        System.out.println("Время поиска без индекса " + timeWithoutIndex + " мс. Найдено строк: " + result1.size());

        System.out.println("\nСоздание B-Tree индекса по колонке 'value'");
        engine.createIndex(tableName, "value");
        long startWithIndex = System.currentTimeMillis();
        List<Map<String, Object>> result2 = engine.select(tableName, List.of("*"), cond);
        long timeWithIndex = System.currentTimeMillis() - startWithIndex;

        System.out.println("Время поиска с индексом (TreeMap): " + timeWithIndex + " мс. Найдено строк: " + result2.size());

        try { engine.dropTable(tableName); } catch (Exception ignored) {}

    }

}