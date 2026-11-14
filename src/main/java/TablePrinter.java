import model.ResultQ;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TablePrinter {

    public static void print(ResultQ result) {
        if (!result.isSuccess) {
            System.out.println(result.message);
            return;
        }
        if (result.columns == null || result.rows == null) {
            System.out.println(result.message);
            return;
        }

        if (result.rows.isEmpty()) {
            System.out.println("Ничего не найдено.");
            return;
        }

        Map<String, Integer> widths = new HashMap<>();

        for (String col : result.columns) {
            widths.put(col, col.length());
        }

        for (Map<String, Object> row : result.rows) {
            for (String col : result.columns) {
                int valLen = row.get(col) == null ? 4 : row.get(col).toString().length();
                widths.put(col, Math.max(widths.get(col), valLen));
            }
        }
        printSeparator(result.columns, widths);
        printHeader(result.columns, widths);
        printSeparator(result.columns, widths);

        for (Map<String, Object> row : result.rows) {
            printRow(result.columns, row, widths);
        }
        printSeparator(result.columns, widths);
        System.out.println("Строк: " + result.rows.size());
    }

    private static void printHeader(List<String> cols, Map<String, Integer> widths) {
        System.out.print("|");
        for (String col : cols) {
            System.out.printf(" %-" + widths.get(col) + "s |", col);
        }
        System.out.println();
    }

    private static void printRow(List<String> cols, Map<String, Object> row, Map<String, Integer> widths) {
        System.out.print("|");
        for (String col : cols) {
            String val = row.get(col) == null ? "NULL" : row.get(col).toString();
            System.out.printf(" %-" + widths.get(col) + "s |", val);
        }
        System.out.println();
    }

    private static void printSeparator(List<String> cols, Map<String, Integer> widths) {
        System.out.print("+");
        for (String col : cols) {
            for (int i = 0; i < widths.get(col) + 2; i++) {
                System.out.print("-");
            }
            System.out.print("+");
        }
        System.out.println();
    }
}