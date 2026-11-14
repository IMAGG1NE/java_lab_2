import model.ResultQ;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Утилита для форматированного вывода результатов SQL-запросов в консоль.
 * рассчитывает ширину колонок на основе длины данных и отрисовывает
 * аккуратную таблицу.
 */
public class TablePrinter {

    /**
     * Выводит результат выполнения команды в стандартный поток вывода .
     * Обрабатывает сообщения об ошибках, пустые результаты и успешно извлеченные данные.
     *
     * @param result объект ResultQ, содержащий статус операции, список колонок и строки данных
     */
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

    /**
     * Печатает строку-заголовок таблицы с названиями колонок.
     *
     * @param cols   список названий колонок
     * @param widths карта с рассчитанной шириной для каждой колонки
     */
    private static void printHeader(List<String> cols, Map<String, Integer> widths) {
        System.out.print("|");
        for (String col : cols) {
            System.out.printf(" %-" + widths.get(col) + "s |", col);
        }
        System.out.println();
    }

    /**
     * Печатает одну строку данных таблицы.
     * Значения {@code null} заменяются на строку "NULL".
     *
     * @param cols   список названий колонок
     * @param row    карта значений текущей строки (имя колонки -> значение)
     * @param widths карта с рассчитанной шириной для каждой колонки
     */
    private static void printRow(List<String> cols, Map<String, Object> row, Map<String, Integer> widths) {
        System.out.print("|");
        for (String col : cols) {
            String val = row.get(col) == null ? "NULL" : row.get(col).toString();
            System.out.printf(" %-" + widths.get(col) + "s |", val);
        }
        System.out.println();
    }

    /**
     * Печатает горизонтальную линию-разделитель таблицы (вида +---+---+).
     *
     * @param cols   список названий колонок
     * @param widths карта с рассчитанной шириной для каждой колонки
     */
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