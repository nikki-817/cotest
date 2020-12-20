package com.company.TokenAnalyse;

public class Token {
    private TokenKind tokenKind;
    private String valStr;
    private int valInt;
    private char valChar;
    private double valDouble;
    private int rowNum;
    private int colNum;
    private boolean isEOF;

    public boolean isEOF() {
        return isEOF;
    }

    public void setEOF(boolean EOF) {
        isEOF = EOF;
    }

    public double getValDouble() {
        return valDouble;
    }

    public int getValInt() {
        return valInt;
    }

    public String getValStr() {
        return valStr;
    }

    public char getValChar() {
        return valChar;
    }

    public TokenKind getTokenKind() {
        return tokenKind;
    }

    public int getRowNum() {
        return rowNum;
    }

    public int getColNum() {
        return colNum;
    }

    public Token(TokenKind tokenKind, double valDouble, int rowNum, int colNum) {
        this.tokenKind = tokenKind;
        this.valDouble = valDouble;
        this.rowNum = rowNum;
        this.colNum = colNum;
    }

    public Token(TokenKind tokenKind, char valChar, int rowNum, int colNum) {
        this.tokenKind = tokenKind;
        this.valChar = valChar;
        this.rowNum = rowNum;
        this.colNum = colNum;
    }

    public Token(TokenKind tokenKind, String valStr, int rowNum, int colNum) {
        this.tokenKind = tokenKind;
        this.valStr = valStr;
        this.rowNum = rowNum;
        this.colNum = colNum;
    }

    public Token(TokenKind tokenKind, int valInt, int rowNum, int colNum) {
        this.tokenKind = tokenKind;
        this.valInt = valInt;
        this.rowNum = rowNum;
        this.colNum = colNum;
    }

    public Token() {

    }


}
