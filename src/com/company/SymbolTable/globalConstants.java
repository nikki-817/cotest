package com.company.SymbolTable;

public class globalConstants{
    private int index;
    private char type;  // I int, D double, S string, C char
    private String val;
    private String name;
    private boolean isConstant;

    public globalConstants(int index, char type, String val, String name, boolean isConstant) {
        this.index = index;
        this.type = type;
        this.val = val;
        this.name = name;
        this.isConstant = isConstant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isConstant() {
        return isConstant;
    }

    public void setConstant(boolean constant) {
        isConstant = constant;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public char getType() {
        return type;
    }

    public void setType(char type) {
        this.type = type;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}
