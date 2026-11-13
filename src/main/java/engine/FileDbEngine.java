package engine;

import model.Condition;
import model.DataType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class FileDbEngine implements DbEngine {

    private static final String DATA_DIR = "data";
    private static final String DELIMITER = "|";

    private final Map<String, Table> tables = new HashMap<>();

    public FileDbEngine() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        loadAllTablesFromDisk();
    }

    @Override
    public void createTable(String table_name, List<String> columns) {
        if (tables.containsKey(table_name)) {
            throw new RuntimeException("Ошибка: Таблица '" + table_name + "' уже существует.");
        }

        Map<String, DataType> schema = new LinkedHashMap<>();
        for (String col : columns) {
            String[] parts = col.split(":");
            if (parts.length != 2) {
                throw new RuntimeException("Ошибка: Неверный формат колонки '" + col + "'. Ожидалось 'имя:ТИП'");
            }
            String colName = parts[0].trim();
            DataType type = DataType.valueOf(parts[1].trim().toUpperCase());
            schema.put(colName, type);
        }

        Table newTable = new Table(table_name, schema);
        tables.put(table_name, newTable);
        saveTableToDisk(newTable);
    }

    @Override
    public void dropTable(String table_name) {
        checkTableExists(table_name);
        new File(DATA_DIR + "/" + table_name + ".schema").delete();
        new File(DATA_DIR + "/" + table_name + ".data").delete();
        tables.remove(table_name);
    }

    @Override
    public void insert(String table_name, Map<String, Object> vals) {
        Table table = checkTableExists(table_name);

        for (Map.Entry<String, Object> entry : vals.entrySet()) {
            String colName = entry.getKey();
            Object value = entry.getValue();

            if (!table.schema.containsKey(colName)) {
                throw new RuntimeException("Ошибка: Колонка '" + colName + "' не найдена в таблице " + table_name);
            }

            if (!isTypeMatching(value, table.schema.get(colName))) {
                throw new RuntimeException("Ошибка: Неверный тип данных для колонки '" + colName + "'");
            }
        }

        table.rows.add(new HashMap<>(vals));
        rebuildIndexes(table);
        saveTableToDisk(table);
    }

    @Override
    public List<Map<String, Object>> select(String table_name, List<String> columns, Condition cond) {
        Table table = checkTableExists(table_name);

        List<Map<String, Object>> matchedRows = getRowsByCondition(table, cond);

        List<Map<String, Object>> result = new ArrayList<>();
        boolean selectAll = columns.contains("*");

        if (selectAll) {
            for (Map<String, Object> row : matchedRows) {
                result.add(new HashMap<>(row));
            }
        } else {
            for (Map<String, Object> row : matchedRows) {
                Map<String, Object> projectedRow = new LinkedHashMap<>();
                for (String col : columns) {
                    if (!table.schema.containsKey(col)) {
                        throw new RuntimeException("Ошибка: Колонка '" + col + "' не найдена");
                    }
                    projectedRow.put(col, row.get(col));
                }
                result.add(projectedRow);
            }
        }
        return result;
    }

    @Override
    public int update(String table_name, Map<String, Object> vals, Condition cond) {
        Table table = checkTableExists(table_name);
        List<Map<String, Object>> matchedRows = getRowsByCondition(table, cond);

        int count = 0;
        for (Map<String, Object> row : matchedRows) {
            for (Map.Entry<String, Object> entry : vals.entrySet()) {
                if (!table.schema.containsKey(entry.getKey())) {
                    throw new RuntimeException("Ошибка: Колонка '" + entry.getKey() + "' не найдена");
                }
                row.put(entry.getKey(), entry.getValue());
            }
            count++;
        }

        if (count > 0) {
            rebuildIndexes(table);
            saveTableToDisk(table);
        }
        return count;
    }

    @Override
    public int delete(String table_name, Condition cond) {
        Table table = checkTableExists(table_name);
        List<Map<String, Object>> matchedRows = getRowsByCondition(table, cond);

        table.rows.removeAll(matchedRows);
        int count = matchedRows.size();

        if (count > 0) {
            rebuildIndexes(table);
            saveTableToDisk(table);
        }
        return count;
    }

    @Override
    public void createIndex(String table_name, String column_name) {
        Table table = checkTableExists(table_name);
        if (!table.schema.containsKey(column_name)) {
            throw new RuntimeException("Ошибка: Колонка '" + column_name + "' не найдена");
        }

        TreeMap<Object, List<Integer>> index = new TreeMap<>();

        for (int i = 0; i < table.rows.size(); i++) {
            Object val = table.rows.get(i).get(column_name);
            if (val != null) {
                index.computeIfAbsent(val, k -> new ArrayList<>()).add(i);
            }
        }

        table.indexes.put(column_name, index);
    }

    private List<Map<String, Object>> getRowsByCondition(Table table, Condition cond) {
        if (cond == null) {
            return new ArrayList<>(table.rows);
        }

        String col = cond.col;

        if (table.indexes.containsKey(col)) {
            TreeMap<Object, List<Integer>> idx = table.indexes.get(col);
            List<Integer> rowIds = new ArrayList<>();
            Object condValue = cond.val;

            switch (cond.operator) {
                case "=":
                    if (idx.containsKey(condValue)) {
                        rowIds.addAll(idx.get(condValue));
                    }
                    break;
                case ">":
                    rowIds.addAll(getIdsFromSubMap(idx.tailMap(condValue, false)));
                    break;
                case "<":
                    rowIds.addAll(getIdsFromSubMap(idx.headMap(condValue, false)));
                    break;
                case ">=":
                    rowIds.addAll(getIdsFromSubMap(idx.tailMap(condValue, true)));
                    break;
                case "<=":
                    rowIds.addAll(getIdsFromSubMap(idx.headMap(condValue, true)));
                    break;
                default:
                    return fullScan(table.rows, cond);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (int id : rowIds) {
                result.add(table.rows.get(id));
            }
            return result;
        }

        return fullScan(table.rows, cond);
    }

    private List<Integer> getIdsFromSubMap(NavigableMap<Object, List<Integer>> subMap) {
        List<Integer> ids = new ArrayList<>();
        for (List<Integer> list : subMap.values()) {
            ids.addAll(list);
        }
        return ids;
    }

    private List<Map<String, Object>> fullScan(List<Map<String, Object>> rows, Condition cond) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object rowVal = row.get(cond.col);
            if (evaluateCondition(rowVal, cond)) {
                result.add(row);
            }
        }
        return result;
    }

    private boolean evaluateCondition(Object rowVal, Condition cond) {
        if (rowVal == null || cond.val == null) return false;

        if (rowVal instanceof Comparable && cond.val instanceof Comparable) {
            Comparable c1 = (Comparable) rowVal;
            Comparable c2 = (Comparable) cond.val;

            if (!c1.getClass().equals(c2.getClass())) return false;

            int cmp = c1.compareTo(c2);
            switch (cond.operator) {
                case "=": return cmp == 0;
                case "!=": return cmp != 0;
                case ">": return cmp > 0;
                case "<": return cmp < 0;
                case ">=": return cmp >= 0;
                case "<=": return cmp <= 0;
            }
        }
        return false;
    }

    private void rebuildIndexes(Table table) {
        for (String colName : table.indexes.keySet()) {
            createIndex(table.name, colName);
        }
    }

    private boolean isTypeMatching(Object value, DataType type) {
        if (value == null) return true;
        switch (type) {
            case INT: return value instanceof Integer;
            case DOUBLE: return value instanceof Double;
            case STRING: return value instanceof String;
            case BOOL: return value instanceof Boolean;
            case DATE: return value instanceof String;
            default: return false;
        }
    }

    private Table checkTableExists(String table_name) {
        if (!tables.containsKey(table_name)) {
            throw new RuntimeException("Ошибка: Таблица '" + table_name + "' не найдена.");
        }
        return tables.get(table_name);
    }

    private void saveTableToDisk(Table table) {
        try {
            try (PrintWriter pw = new PrintWriter(DATA_DIR + "/" + table.name + ".schema")) {
                String schemaLine = table.schema.entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue().name())
                        .collect(Collectors.joining(DELIMITER));
                pw.println(schemaLine);
            }

            try (PrintWriter pw = new PrintWriter(DATA_DIR + "/" + table.name + ".data")) {
                List<String> cols = new ArrayList<>(table.schema.keySet());
                for (Map<String, Object> row : table.rows) {
                    String line = cols.stream()
                            .map(c -> row.get(c) == null ? "NULL" : row.get(c).toString())
                            .collect(Collectors.joining(DELIMITER));
                    pw.println(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка записи на диск: " + e.getMessage());
        }
    }

    private void loadAllTablesFromDisk() {
        File dir = new File(DATA_DIR);
        File[] schemaFiles = dir.listFiles((d, name) -> name.endsWith(".schema"));

        if (schemaFiles != null) {
            for (File schemaFile : schemaFiles) {
                String tableName = schemaFile.getName().replace(".schema", "");
                try {
                    Map<String, DataType> schema = new LinkedHashMap<>();
                    List<String> lines = Files.readAllLines(Paths.get(schemaFile.getPath()));
                    if (!lines.isEmpty()) {
                        String[] parts = lines.get(0).split("\\|");
                        for (String p : parts) {
                            String[] kv = p.split(":");
                            schema.put(kv[0], DataType.valueOf(kv[1]));
                        }
                    }

                    Table table = new Table(tableName, schema);

                    File dataFile = new File(DATA_DIR + "/" + tableName + ".data");
                    if (dataFile.exists()) {
                        List<String> dataLines = Files.readAllLines(dataFile.toPath());
                        List<String> cols = new ArrayList<>(schema.keySet());

                        for (String line : dataLines) {
                            if (line.trim().isEmpty()) continue;
                            String[] vals = line.split("\\|");
                            Map<String, Object> row = new HashMap<>();

                            for (int i = 0; i < cols.size(); i++) {
                                String valStr = vals[i];
                                if (!valStr.equals("NULL")) {
                                    row.put(cols.get(i), parseValue(valStr, schema.get(cols.get(i))));
                                } else {
                                    row.put(cols.get(i), null);
                                }
                            }
                            table.rows.add(row);
                        }
                    }
                    tables.put(tableName, table);
                } catch (Exception e) {
                    System.err.println("Не удалось загрузить таблицу " + tableName + ": " + e.getMessage());
                }
            }
        }
    }

    private Object parseValue(String val, DataType type) {
        switch (type) {
            case INT: return Integer.parseInt(val);
            case DOUBLE: return Double.parseDouble(val);
            case BOOL: return Boolean.parseBoolean(val);
            case STRING:
            case DATE: return val;
            default: return val;
        }
    }

    private static class Table {
        String name;
        Map<String, DataType> schema;
        List<Map<String, Object>> rows;
        Map<String, TreeMap<Object, List<Integer>>> indexes;

        Table(String name, Map<String, DataType> schema) {
            this.name = name;
            this.schema = new LinkedHashMap<>(schema);
            this.rows = new ArrayList<>();
            this.indexes = new HashMap<>();
        }
    }
}