package com.company.error.GrammerError;

public class GrammerError extends Exception {

    public GrammerError(int errRow, int errCol, String message) {
        super("Line: " + errRow + ", Column: " + errCol + " " + message);
    }

    public GrammerError(){

    }

}
