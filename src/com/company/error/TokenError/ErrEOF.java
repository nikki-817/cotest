package com.company.error.TokenError;

public class ErrEOF extends TokenError {
    public ErrEOF(int errRow, int errCol, String message) {
        super(errRow, errCol, message);
    }
}
