package com.company.Util;

import com.company.TokenAnalyse.Token;

/**
 * 一些实用工具，判断单个字符类型
 */

public class Util {
    public static boolean isBlank(char c){
        return (c == ' ' || c == '\r' || c == '\t' || c == '\n');
    }

    public static boolean isDigit(char c){
        return (c >= '0' && c <= '9');
    }

    public static boolean isAlpha(char c){
        return (c >= 'a' && c <= 'z' || c >= 'A' && c<= 'Z');
    }

    public static boolean isPrint(char c){
        return (c >= 0x20 && c <= 0x7E);
    }

    public static boolean isCchar(char c ){
        return ((isBlank(c) && c != '\r' && c!= '\n') || isDigit(c) || isAlpha(c) || (isPunctuation(c) && c != '\'' && c != '\\'));
    }

    public static boolean isSchar(char c){
        return ((isBlank(c) && c != '\r' && c!= '\n') || isDigit(c) || isAlpha(c) || (isPunctuation(c) && c != '\"' && c != '\\'));

    }

    public static boolean isEscape(char c){
        return c == '\\' || c == '\'' || c == '\"' || c == '\n' || c == '\r' || c == '\t';
    }

    public static boolean isPunctuation(char c){
        return (c == '_' || c == '(' || c == ')' || c == '[' ||
                c == ']' || c == '{' || c == '}' || c == '<' ||
                c == '=' || c == '>' || c == '.' || c == ',' ||
                c == ':' || c == ';' || c == '!' || c == '?' ||
                c == '+' || c == '-' || c == '*' || c == '/' ||
                c == '%' || c == '^' || c == '&' || c == '|' ||
                c == '~' || c == '\\'|| c == '\"'|| c == '\''||
                c == '`' || c == '$' || c == '#' || c == '@'
        );
    }

    public static boolean isAcceptable(char c){
        return isPunctuation(c) || isBlank(c) || isDigit(c) || isAlpha(c);
    }

    public static char getTwoHexVal(char a, char b){
        return (char) (getHexCharVal(a) * 16 + getHexCharVal(b));
    }

    private static int getHexCharVal(char ch){
        if(ch >= 'a' && ch <= 'f')
            return ch - 'a' + 10;
        if(ch >= 'A' && ch <= 'F')
            return ch - 'A' + 10;
        else
            return ch - '0';
    }




}
