package com.company.SymbolTable;


import java.util.ArrayList;

public class globalFunctions{
    private int index;
    private int name_index;
    private int params_size;
    private int level;
    private int localsLen;
    private RtnType rtnType;
    private ArrayList<ArgsInfo> args = new ArrayList<>();
    private String name;
    private  int count; //局部变量的数量


    public globalFunctions(int index, int name_index, int params_size, int level, ArrayList<ArgsInfo> args, int localsLen, RtnType type, String name) {
        this.index = index;
        this.name_index = name_index;
        this.params_size = params_size;
        this.level = level;
        this.args = args;
        this.localsLen = localsLen;
        this.rtnType = type;
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RtnType getRtnType() {
        return rtnType;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getName_index() {
        return name_index;
    }

    public void setName_index(int name_index) {
        this.name_index = name_index;
    }

    public int getParams_size() {
        return params_size;
    }

    public void setParams_size(int params_size) {
        this.params_size = params_size;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
