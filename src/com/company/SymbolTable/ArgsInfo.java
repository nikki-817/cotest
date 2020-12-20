package com.company.SymbolTable;

public class ArgsInfo {
    private boolean isConstant;
    private ValType type;
    private String name;
    private int index;

    public ArgsInfo(boolean isConstant, ValType type, String name, int index) {
        this.isConstant = isConstant;
        this.type = type;
        this.name = name;
        this.index = index;
    }

    public boolean isConstant() {
        return isConstant;
    }

    public ValType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }
}
