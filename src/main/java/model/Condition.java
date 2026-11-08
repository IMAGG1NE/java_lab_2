package model;

public class Condition{
    public final String col;
    public final String operator;
    public final Object val;

    public Condition(String col, String op, Object val){
        this.col = col;
        this.operator = op;
        this.val = val;
    }
}