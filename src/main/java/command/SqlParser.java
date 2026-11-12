package command;

import model.Condition;
import model.DataType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlParser {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;

    private static final Pattern CREATE_TABLE =
            Pattern.compile("^CREATE\\s+TABLE\\s+(\\w+)\\s*\\((.+)\\)$", FLAGS);
    private static final Pattern CREATE_INDEX =
            Pattern.compile("^CREATE\\s+INDEX\\s+(\\w+)\\s+ON\\s+(\\w+)\\s*\\(\\s*(\\w+)\\s*\\)$", FLAGS);
    private static final Pattern DROP_TABLE =
            Pattern.compile("^DROP\\s+TABLE\\s+(\\w+)$", FLAGS);
    private static final Pattern INSERT =
            Pattern.compile("^INSERT\\s+INTO\\s+(\\w+)\\s*(?:\\(([^)]*)\\))?\\s*VALUES\\s*(.+)$", FLAGS);
    private static final Pattern SELECT =
            Pattern.compile("^SELECT\\s+(.+?)\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.+))?$", FLAGS);
    private static final Pattern UPDATE =
            Pattern.compile("^UPDATE\\s+(\\w+)\\s+SET\\s+(.+?)(?:\\s+WHERE\\s+(.+))?$", FLAGS);
    private static final Pattern DELETE =
            Pattern.compile("^DELETE\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.+))?$", FLAGS);

    private static final Pattern WHERE_EXPR =
            Pattern.compile("^(\\w+)\\s*(>=|<=|!=|<>|=|>|<)\\s*(.+)$", FLAGS);
    private static final Pattern ASSIGN =
            Pattern.compile("^(\\w+)\\s*=\\s*(.+)$", FLAGS);

    private static final Pattern INT_LITERAL = Pattern.compile("[+-]?\\d+");
    private static final Pattern DOUBLE_LITERAL = Pattern.compile("[+-]?\\d+\\.\\d+");
    private static final Pattern IDENT = Pattern.compile("\\w+");

    private SqlParser() {
    }

    public static Command parse(String sql) {
        try {
            return doParse(sql);
        } catch (SqlSyntaxException e) {
            return new ErrorCommand(e.getMessage());
        } catch (RuntimeException e) {
            return new ErrorCommand("непредвиденная ошибка разбора: " + e);
        }
    }

    private static Command doParse(String sql) {
        if (sql == null) {
            throw new SqlSyntaxException("пустой запрос");
        }
        String q = sql.trim();
        while (q.endsWith(";")) {
            q = q.substring(0, q.length() - 1).trim();
        }
        if (q.isEmpty()) {
            throw new SqlSyntaxException("пустой запрос");
        }

        Matcher m;
        if ((m = CREATE_TABLE.matcher(q)).matches()) return parseCreateTable(m);
        if ((m = CREATE_INDEX.matcher(q)).matches())
            return new CreateIndexCommand(m.group(1), m.group(2), m.group(3));
        if ((m = DROP_TABLE.matcher(q)).matches()) return new DropTableCommand(m.group(1));
        if ((m = INSERT.matcher(q)).matches()) return parseInsert(m);
        if ((m = SELECT.matcher(q)).matches()) return parseSelect(m);
        if ((m = UPDATE.matcher(q)).matches()) return parseUpdate(m);
        if ((m = DELETE.matcher(q)).matches())
            return new DeleteCommand(m.group(1), parseWhere(m.group(2)));

        throw new SqlSyntaxException("неизвестная или некорректная команда: " + q);
    }

    private static Command parseCreateTable(Matcher m) {
        Map<String, DataType> columns = new LinkedHashMap<>();
        for (String def : splitTopLevel(m.group(2))) {
            String[] p = def.trim().split("\\s+");
            if (p.length < 2 || !IDENT.matcher(p[0]).matches()) {
                throw new SqlSyntaxException("неверное описание колонки: '" + def + "'");
            }
            DataType type = parseType(p[1]);
            boolean pk = false;
            if (p.length == 4 && p[2].equalsIgnoreCase("PRIMARY") && p[3].equalsIgnoreCase("KEY")) {
                pk = true;
            } else if (p.length != 2) {
                throw new SqlSyntaxException("неверное описание колонки: '" + def + "'");
            }
            if (pk && !columns.isEmpty()) {
                throw new SqlSyntaxException("PRIMARY KEY может быть только у первой колонки");
            }
            if (columns.put(p[0], type) != null) {
                throw new SqlSyntaxException("колонка '" + p[0] + "' объявлена дважды");
            }
        }
        return new CreateTableCommand(m.group(1), columns);
    }

    private static Command parseInsert(Matcher m) {
        List<String> cols = null;
        if (m.group(2) != null) {
            cols = new ArrayList<>();
            for (String c : splitTopLevel(m.group(2))) {
                cols.add(checkIdent(c));
            }
        }
        List<List<Object>> rows = new ArrayList<>();
        for (String group : splitTopLevel(m.group(3))) {
            if (!(group.startsWith("(") && group.endsWith(")"))) {
                throw new SqlSyntaxException("значения должны быть в скобках: '" + group + "'");
            }
            List<Object> row = new ArrayList<>();
            for (String v : splitTopLevel(group.substring(1, group.length() - 1))) {
                row.add(parseLiteral(v));
            }
            if (cols != null && row.size() != cols.size()) {
                throw new SqlSyntaxException("число колонок (" + cols.size()
                        + ") не совпадает с числом значений (" + row.size() + ")");
            }
            rows.add(row);
        }
        return new InsertCommand(m.group(1), cols, rows);
    }

    private static Command parseSelect(Matcher m) {
        List<String> cols = new ArrayList<>();
        String raw = m.group(1).trim();
        if (raw.equals("*")) {
            cols.add("*");
        } else {
            for (String c : splitTopLevel(raw)) {
                cols.add(checkIdent(c));
            }
        }
        return new SelectCommand(m.group(2), cols, parseWhere(m.group(3)));
    }

    private static Command parseUpdate(Matcher m) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String part : splitTopLevel(m.group(2))) {
            Matcher a = ASSIGN.matcher(part);
            if (!a.matches()) {
                throw new SqlSyntaxException("ожидалось 'колонка = значение', получено: '" + part + "'");
            }
            values.put(a.group(1), parseLiteral(a.group(2)));
        }
        return new UpdateCommand(m.group(1), values, parseWhere(m.group(3)));
    }

    private static Condition parseWhere(String where) {
        if (where == null) {
            return null;
        }
        Matcher m = WHERE_EXPR.matcher(where.trim());
        if (!m.matches()) {
            throw new SqlSyntaxException("неверное условие WHERE: '" + where + "'");
        }
        String op = m.group(2).equals("<>") ? "!=" : m.group(2);
        return new Condition(m.group(1), op, parseLiteral(m.group(3)));
    }

    private static DataType parseType(String name) {
        switch (name.toUpperCase()) {
            case "INT":
            case "INTEGER":
                return DataType.INT;
            case "DOUBLE":
            case "FLOAT":
                return DataType.DOUBLE;
            case "STRING":
            case "TEXT":
            case "VARCHAR":
                return DataType.STRING;
            case "DATE":
                return DataType.DATE;
            case "BOOL":
            case "BOOLEAN":
                return DataType.BOOL;
            default:
                throw new SqlSyntaxException("неизвестный тип данных: " + name);
        }
    }

    static Object parseLiteral(String raw) {
        String t = raw.trim();
        if (t.isEmpty()) {
            throw new SqlSyntaxException("пустое значение");
        }
        if (t.length() >= 2 && t.startsWith("'") && t.endsWith("'")) {
            return t.substring(1, t.length() - 1).replace("''", "'");
        }
        if (t.equalsIgnoreCase("TRUE")) return Boolean.TRUE;
        if (t.equalsIgnoreCase("FALSE")) return Boolean.FALSE;
        if (t.equalsIgnoreCase("NULL")) return null;
        try {
            if (INT_LITERAL.matcher(t).matches()) return Integer.valueOf(t);
            if (DOUBLE_LITERAL.matcher(t).matches()) return Double.valueOf(t);
        } catch (NumberFormatException e) {
            throw new SqlSyntaxException("число вне допустимого диапазона: " + t);
        }
        throw new SqlSyntaxException("не удалось распознать значение: " + t);
    }

    private static String checkIdent(String s) {
        String t = s.trim();
        if (!IDENT.matcher(t).matches()) {
            throw new SqlSyntaxException("неверное имя: '" + t + "'");
        }
        return t;
    }

    static List<String> splitTopLevel(String s) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && c == '(') {
                depth++;
            } else if (!inQuotes && c == ')') {
                depth--;
            } else if (!inQuotes && depth == 0 && c == ',') {
                parts.add(cur.toString().trim());
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        parts.add(cur.toString().trim());
        return parts;
    }
}