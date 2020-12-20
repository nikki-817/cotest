package com.company.error.TokenError;

public class TokenError extends Exception {
    public TokenError(int errRow, int errCol, String message) {
        super("Line: " + errRow + ", Column: " + errCol + " " + message);
    }

}
