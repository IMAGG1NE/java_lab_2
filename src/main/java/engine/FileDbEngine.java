package engine;

import model.Condition;
import model.DataType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация ядра системы управления базами данных.
 * Обеспечивает хранение таблиц на диске (в файлах .schema и .data),
 * строгую проверку типов данных, контроль уникальности первичного ключа
 * и поддержку B-Tree индексов для оптимизации поисковых запросов.
 */
public class FileDbEngine implements DbEngine {

    private static final String DATA_DIR = "data";
    private static final String DELIMITER = "|";

    private final Map<String, Table> tables = new HashMap<>();

    /**
     * Конструктор движка СУБД.
     * Создает директорию для хранения данных (если она не существует)
     * и автоматически загружает все существующие таблицы с диска в оперативную память.
     */
    public FileDbEngine() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        loadAllTablesFromDisk();
    }

    /**
     * Создает новую таблицу с заданной схемой и сохраняет её на диск.
     *
     * @param table_name название создаваемой таблицы
     * @param columns    карта, описывающая схему таблицы (имя колонки -> тип данных)
     * @throws RuntimeException если таблица с таким именем уже существует
     */
    @Override
    public void createTable(String table_name, Map<String, DataType> columns) {
        if (tables.containsKey(table_name)) {
            throw new RuntimeException("Ошибка: Таблица '" + table_name + "' уже существует.");
        }

        Table newTable = new Table(table_name, new LinkedHashMap<>(columns));
        tables.put(table_name, newTable);
        saveTableToDisk(newTable);
    }

    /**
     * Возвращает список названий всех колонок указанной таблицы.
     *
     * @param table_name название таблицы
     * @return список имен колонок в порядке их объявления в схеме
     * @throws RuntimeException если таблица не найдена
     */
    @Override
    public List<String> getColumnNames(String table_name) {
        Table table = checkTableExists(table_name);
        return new ArrayList<>(table.schema.keySet());
    }

    /**
     * Полностью удаляет таблицу из оперативной памяти, а также стирает её файлы (.schema и .data) с диска.
     *
     * @param table_name название удаляемой таблицы
     * @throws RuntimeException если таблица не найдена
     */
    @Override
    public void dropTable(String table_name) {
        checkTableExists(table_name);
        new File(DATA_DIR + "/" + table_name + ".schema").delete();
        new File(DATA_DIR + "/" + table_name + ".data").delete();
        tables.remove(table_name);
    }

    /**
     * Вставляет новую запись (строку) в указанную таблицу.
     * Проверяет существование колонок, соответствие типов данных и уникальность первичного ключа (первой колонки).
     * После вставки перестраивает индексы и сохраняет изменения на диск.
     *
     * @param table_name название целевой таблицы
     * @param vals       карта значений для вставки (имя колонки -> значение)
     * @throws RuntimeException если колонка не найдена, тип данных некорректен или нарушена уникальность первичного ключа
     */
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

            if (table.schema.get(colName) == DataType.DOUBLE && value instanceof Integer) {
                entry.setValue(((Integer) value).doubleValue());
            }
        }

        if (!table.schema.isEmpty()) {
            String pkCol = table.schema.keySet().iterator().next();
            Object pkVal = vals.get(pkCol);
            if (pkVal != null) {
                for (Map<String, Object> row : table.rows) {
                    if (pkVal.equals(row.get(pkCol))) {
                        throw new RuntimeException("Ошибка: Дубликат первичного ключа '" + pkCol + "' = " + pkVal);
                    }
                }
            }
        }

        table.rows.add(new HashMap<>(vals));
        rebuildIndexes(table);
        saveTableToDisk(table);
    }

    /**
     * Выполняет выборку данных из таблицы с возможностью проекции колонок и фильтрации по условию.
     *
     * @param table_name название таблицы
     * @param columns    список запрашиваемых колонок (или "*" для выбора всех колонок)
     * @param cond       условие фильтрации WHERE (может быть null, если фильтрация не требуется)
     * @return список строк, удовлетворяющих условию
     * @throws RuntimeException если таблица или одна из запрашиваемых колонок не найдены
     */
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

    /**
     * Обновляет значения в строках, удовлетворяющих заданному условию.
     * При наличии изменений перестраивает индексы и перезаписывает файл данных на диске.
     *
     * @param table_name название таблицы
     * @param vals       карта новых значений (имя колонки -> новое значение)
     * @param cond       условие отбора строк для обновления (может быть null для обновления всех строк)
     * @return количество обновленных записей
     * @throws RuntimeException если таблица/колонка не найдены или передан неверный тип данных
     */
    @Override
    public int update(String table_name, Map<String, Object> vals, Condition cond) {
        Table table = checkTableExists(table_name);
        List<Map<String, Object>> matchedRows = getRowsByCondition(table, cond);

        int count = 0;
        for (Map<String, Object> row : matchedRows) {
            for (Map.Entry<String, Object> entry : vals.entrySet()) {
                String colName = entry.getKey();
                Object value = entry.getValue();
                if (!table.schema.containsKey(colName)) {
                    throw new RuntimeException("Ошибка: Колонка '" + colName + "' не найдена");
                }
                if (!isTypeMatching(value, table.schema.get(colName))) {
                    throw new RuntimeException("Ошибка: Неверный тип данных для колонки '" + colName + "'");
                }
                if (table.schema.get(colName) == DataType.DOUBLE && value instanceof Integer) {
                    value = ((Integer) value).doubleValue();
                }
                row.put(colName, value);
            }
            count++;
        }

        if (count > 0) {
            rebuildIndexes(table);
            saveTableToDisk(table);
        }
        return count;
    }

    /**
     * Удаляет из таблицы строки, удовлетворяющие заданному условию.
     * При наличии удаленных строк перестраивает индексы и обновляет файл на диске.
     *
     * @param table_name название таблицы
     * @param cond       условие для отбора удаляемых строк (может быть null для очистки всей таблицы)
     * @return количество удаленных записей
     * @throws RuntimeException если таблица не найдена
     */
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

    /**
     * Создает B-Tree индекс в оперативной памяти для указанной колонки.
     * Позволяет ускорить операции выборки, обновления и удаления с условием WHERE по данной колонке.
     *
     * @param table_name  название таблицы
     * @param column_name название индексируемой колонки
     * @throws RuntimeException если таблица или колонка не найдены
     */
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

    /**
     * Возвращает список строк таблицы, подходящих под условие фильтрации.
     * Если по колонке из условия построен индекс и оператор поддерживается, выполняет быстрый поиск по дереву,
     * иначе делегирует выполнение полному сканированию таблицы.
     *
     * @param table целевая таблица
     * @param cond  условие фильтрации (может быть null)
     * @return список отфильтрованных строк
     */
    private List<Map<String, Object>> getRowsByCondition(Table table, Condition cond) {
        if (cond == null) {
            return new ArrayList<>(table.rows);
        }

        String col = cond.col;
        if (!table.schema.containsKey(col)) {
            throw new RuntimeException("Ошибка: Колонка '" + col + "' не найдена");
        }

        if (table.indexes.containsKey(col)) {
            TreeMap<Object, List<Integer>> idx = table.indexes.get(col);
            List<Integer> rowIds = new ArrayList<>();
            Object condValue = cond.val;
            if (table.schema.get(col) == DataType.DOUBLE && condValue instanceof Integer) {
                condValue = ((Integer) condValue).doubleValue();
            }

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

    /**
     * Извлекает и объединяет списки индексов строк из поддерева.
     *
     * @param subMap срез дерева индекса
     * @return плоский список номеров строк
     */
    private List<Integer> getIdsFromSubMap(NavigableMap<Object, List<Integer>> subMap) {
        List<Integer> ids = new ArrayList<>();
        for (List<Integer> list : subMap.values()) {
            ids.addAll(list);
        }
        return ids;
    }

    /**
     * Выполняет последовательный перебор всех строк таблицы с проверкой условия.
     *
     * @param rows все строки таблицы
     * @param cond условие отбора
     * @return список строк, удовлетворяющих условию
     */
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

    /**
     * Проверяет, удовлетворяет ли конкретное значение ячейки заданному условию.
     * Корректно сравнивает числовые типы и прочие сравнимые объекты.
     *
     * @param rowVal значение из строки таблицы
     * @param cond   условие с оператором и целевым значением
     * @return true, если условие выполняется, иначе false
     */
    private boolean evaluateCondition(Object rowVal, Condition cond) {
        if (rowVal == null || cond.val == null) return false;

        if (rowVal instanceof Number && cond.val instanceof Number) {
            int cmp = Double.compare(((Number) rowVal).doubleValue(), ((Number) cond.val).doubleValue());
            switch (cond.operator) {
                case "=": return cmp == 0;
                case "!=": return cmp != 0;
                case ">": return cmp > 0;
                case "<": return cmp < 0;
                case ">=": return cmp >= 0;
                case "<=": return cmp <= 0;
            }
            return false;
        }

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

    /**
     * Перестраивает все существующие индексы для указанной таблицы.
     * Вызывается после операций модификации данных (insert, update, delete).
     *
     * @param table таблица, индексы которой необходимо обновить
     */
    private void rebuildIndexes(Table table) {
        for (String colName : table.indexes.keySet()) {
            createIndex(table.name, colName);
        }
    }

    /**
     * Проверяет соответствие переданного объекта Java ожидаемому типу данных СУБД.
     *
     * @param value проверяемое значение (может быть null)
     * @param type  целевой тип данных из схемы таблицы
     * @return true, если тип значения допустим, иначе false
     */
    private boolean isTypeMatching(Object value, DataType type) {
        if (value == null) return true;
        switch (type) {
            case INT: return value instanceof Integer;
            case DOUBLE: return value instanceof Double || value instanceof Integer;
            case STRING: return value instanceof String;
            case BOOL: return value instanceof Boolean;
            case DATE: return value instanceof String;
            default: return false;
        }
    }

    /**
     * Проверяет существование таблицы в памяти и возвращает её объект.
     *
     * @param table_name название проверяемой таблицы
     * @return внутренний объект {@link Table}
     * @throws RuntimeException если таблица не существует
     */
    private Table checkTableExists(String table_name) {
        if (!tables.containsKey(table_name)) {
            throw new RuntimeException("Ошибка: Таблица '" + table_name + "' не найдена.");
        }
        return tables.get(table_name);
    }

    /**
     * Сериализует и сохраняет схему таблицы (в файл .schema) и её строки (в файл .data) на жесткий диск.
     *
     * @param table таблица для сохранения
     * @throws RuntimeException при возникновении ошибок ввода-вывода
     */
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

    /**
     * Сканирует директорию данных при запуске СУБД, читает файлы схем и данных,
     * восстанавливая структуру таблиц и их содержимое в оперативной памяти.
     */
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

    /**
     * Преобразует строковое представление значения, прочитанное из файла на диске,
     * в соответствующий типизированный объект Java.
     *
     * @param val  строковое значение из файла .data
     * @param type тип данных колонки согласно схеме
     * @return типизированный объект (Integer, Double, Boolean или String)
     */
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

    /**
     * Внутренний класс-контейнер, представляющий структуру отдельной таблицы в памяти.
     * Хранит имя, схему колонок, список записей и построенные индексы.
     */
    private static class Table {
        String name;
        Map<String, DataType> schema;
        List<Map<String, Object>> rows;
        Map<String, TreeMap<Object, List<Integer>>> indexes;

        /**
         * Создает новый экземпляр таблицы в памяти.
         *
         * @param name   имя таблицы
         * @param schema карта колонок и их типов данных
         */
        Table(String name, Map<String, DataType> schema) {
            this.name = name;
            this.schema = new LinkedHashMap<>(schema);
            this.rows = new ArrayList<>();
            this.indexes = new HashMap<>();
        }
    }
}