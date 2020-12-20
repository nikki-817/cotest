package com.company.TokenAnalyse;

/**
 * 词法分析自动机的状态，分为基础c0部分和扩展c0部分
 */

public enum TokenAnalyseState {

    /**
     * 基础c0部分
     */
    INITSTATE,
    ZEROSTATE,  // 读了一个0，可能只有一个0，也有可能是16进制，也有可能是个浮点数
    DIGITSTATE, // 读了整数数字

    IDENTIFERSTATE, // 读入标识符

    ADDSIGNSTATE,
    MINUSSIGNSTATE, // 可能出现-或者->
    MULTSIGNSTATE,
    DIVSIGNSTATE,

    SMALLERSTATE,   //可能会出现<=
    BIGGERSTATE,    //可能会出现>=
    NOTSTATE,   //可能会出现!=
    ASSIGNSTATE, //可能会出现==


    LEFTBRACESSTATE, // {
    RIGHTBRACESSTATE, // }
    LEFTPARENTHESESTATE, // (
    RIGHTPARENTHESESTATE, // )
    SEMICOLONSTATE, // ;
    COMMASTATE, // ,
    COLONSTATE, // :

    CHARVALSTATE,   // 字符字面量
    STRINGVALSTATE, // 字符串字面量
    DOUBLEVALSTATE, // 浮点字面量

}
