package model;

/**
 * Класс-контейнер, представляющий условие фильтрации аналог секции WHERE в SQL.
 * Хранит информацию о целевой колонке, операторе сравнения и значении.
 */
public class Condition {

    /**
     * Имя колонки, по которой происходит фильтрация.
     */
    public final String col;

    /**
     * Оператор сравнения (например: "=", ">", "<", ">=", "<=", "!=").
     */
    public final String operator;

    /**
     * Значение для сравнения с данными в колонке.
     */
    public final Object val;

    /**
     * Инициализирует новое условие фильтрации.
     *
     * @param col имя колонки
     * @param op  оператор сравнения
     * @param val значение для сравнения
     */
    public Condition(String col, String op, Object val) {
        this.col = col;
        this.operator = op;
        this.val = val;
    }
}