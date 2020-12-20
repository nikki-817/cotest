package com.company.SymbolTable;

public class SymbolEntry {
    private String name;
    private int intVal;
    private double doubleVal;
    private char charVal;
    private String strVal;
    private SymbolType type;
    private int level;
    private int offset; //记录是函数的第几个参数
    private ValType tkType;
    private boolean isChar;
    private boolean isInteger;

    public boolean isChar() {
        return isChar;
    }

    public boolean isInteger() {
        return isInteger;
    }

    public SymbolEntry(){

    }

    public SymbolEntry(String name, int intVal, SymbolType type, int level, ValType tkType, int offset) {
        this.name = name;
        this.intVal = intVal;
        this.type = type;
        this.level = level;
        this.tkType = tkType;
        this.offset = offset;
        this.isInteger = true;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIntVal() {
        return intVal;
    }

    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public double getDoubleVal() {
        return doubleVal;
    }

    public void setDoubleVal(double doubleVal) {
        this.doubleVal = doubleVal;
    }

    public char getCharVal() {
        return charVal;
    }

    public void setCharVal(char charVal) {
        this.charVal = charVal;
    }

    public String getStrVal() {
        return strVal;
    }

    public void setStrVal(String strVal) {
        this.strVal = strVal;
    }

    public SymbolType getType() {
        return type;
    }

    public void setType(SymbolType type) {
        this.type = type;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
