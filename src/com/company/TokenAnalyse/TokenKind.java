package com.company.TokenAnalyse;

public enum TokenKind {
    // 以下是基础c0
    // 标识符
    IDENT,
    // 关键字
    FN_KW,
    LET_KW,
    CONST_KW,
    AS_KW,
    WHILE_KW,
    IF_KW,
    ELSE_KW,
    RETURN_KW,
    INT,
    VOID,
    DOUBLE,

    //字面量
    //UINT_LITERAL, //无符号整数，全部放在INT里
    STRING_LITERAL, //字符串常量
    INT_LITERAL,

    //运算符,加减乘除
    PLUS,    // +
    MINUS,  // -
    MUL,   // *
    DIV,    // /
    // 赋值
    ASSIGN, // =

    // 大于，小于，大于等于，小于等于，等于
    GT,
    LT,
    GE,
    LE,
    EQUAL, // ==
    NEQ, //!=

    // 各种符号
    L_BRACE, // {
    R_BRACE, // }
    L_PAREN, // (
    R_PAREN, // )
    SEMICOLON, // ;
    COMMA,  // ,
    COLON,  // :
    ARROW, // ->

    //标准库函数
    GETINT,
    GETCHAR,
    PUTINT,
    PUTCHAR,
    PUTSTR,
    PUTLN,

    // 以下是扩展c0
    BREAK_KW,
    CONTINUE_KW,
    DOUBLE_LITERAL, //浮点数常量
    SINGLEANNOTATION, // 单行注释

}
