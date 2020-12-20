package com.company.TokenAnalyse;

import com.company.Util.Pair;
import com.company.Util.Util;
import com.company.error.TokenError.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Optional;

/**
 * 表示位置的类
 */

class Pos {
    private int row;
    private int col;

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public Pos(int row, int col) {
        this.row = row;
        this.col = col;
    }


}

/**
 * 词法分析类
 */

public class TokenAnalyse {

    private Pos posToken = new Pos(0, 0);
    private Pos pos = new Pos(0, 0);
    private ArrayList<String> input = new ArrayList<>();

    /*
    public void DoTokenAnalyse() {
        Pair<Optional<Token>, Optional<Exception>> tk = NextToken();
        while (!tk.second.isPresent()) {

            if (tk.first.isPresent() && tk.first.get().getTokenKind() == TokenKind.IDENTIFER)
                System.out.println("Row: " + tk.first.get().getRowNum() + " Col: " + tk.first.get().getColNum() + " " + tk.first.get().getTokenKind() + " " + tk.first.get().getValStr());
            else if (tk.first.isPresent() && tk.first.get().getTokenKind() == TokenKind.DOUBLEVAL)
                System.out.println("Row: " + tk.first.get().getRowNum() + " Col: " + tk.first.get().getColNum() + " " + tk.first.get().getTokenKind() + " " + tk.first.get().getValDouble());
            else if (tk.first.isPresent() && tk.first.get().getTokenKind() == TokenKind.INTERGERVAL)
                System.out.println("Row: " + tk.first.get().getRowNum() + " Col: " + tk.first.get().getColNum() + " " + tk.first.get().getTokenKind() + " " + tk.first.get().getValInt());
            else if (tk.first.isPresent() && tk.first.get().getTokenKind() == TokenKind.CHARVAL)
                System.out.println("Row: " + tk.first.get().getRowNum() + " Col: " + tk.first.get().getColNum() + " " + tk.first.get().getTokenKind() + " " + tk.first.get().getValChar());
            else if (tk.first.isPresent())
                System.out.println("Row: " + tk.first.get().getRowNum() + " Col: " + tk.first.get().getColNum() + " " + tk.first.get().getTokenKind() + " " + tk.first.get().getValStr());

            tk = NextToken();
        }
        try {
            throw tk.second.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    */

    /**
     * 通过维护一个自动机来实现词法分析，每次先读入一个字符
     * 注意如果读到的单词是单个字符需要回退
     *
     * @return 返回一个Token
     */

    public Pair<Optional<Token>, Optional<Exception>> NextToken() {
        StringBuilder tkStr = new StringBuilder();
        Token tk = new Token();
        TokenAnalyseState state = TokenAnalyseState.INITSTATE;
        boolean escape = false;
        boolean escapeChar = false;


        while (true) {
            char nextChar = readChar();
            switch (state) {
                case INITSTATE: {
                    if (isEOF())
                        return new Pair<>(Optional.empty(), Optional.of(new ErrEOF(posToken.getRow(), posToken.getCol(), "EOF")));
                    boolean invalid = false;
                    if (Util.isBlank(nextChar))
                        break;
                    else if (!Util.isPrint(nextChar))
                        invalid = true;
                    else if (nextChar == '0')
                        state = TokenAnalyseState.ZEROSTATE;
                    else if (Util.isDigit(nextChar))
                        state = TokenAnalyseState.DIGITSTATE;
                    else if (Util.isAlpha(nextChar) || nextChar == '_')
                        state = TokenAnalyseState.IDENTIFERSTATE;
                    else {
                        switch (nextChar) {
                            case '+':
                                state = TokenAnalyseState.ADDSIGNSTATE;
                                break;
                            case '-':
                                state = TokenAnalyseState.MINUSSIGNSTATE;
                                break;
                            case '*':
                                state = TokenAnalyseState.MULTSIGNSTATE;
                                break;
                            case '/':
                                state = TokenAnalyseState.DIVSIGNSTATE;
                                break;
                            case '<':
                                state = TokenAnalyseState.SMALLERSTATE;
                                break;
                            case '>':
                                state = TokenAnalyseState.BIGGERSTATE;
                                break;
                            case '!':
                                state = TokenAnalyseState.NOTSTATE;
                                break;
                            case '=':
                                state = TokenAnalyseState.ASSIGNSTATE;
                                break;
                            case '{':
                                state = TokenAnalyseState.LEFTBRACESSTATE;
                                break;
                            case '}':
                                state = TokenAnalyseState.RIGHTBRACESSTATE;
                                break;
                            case '(':
                                state = TokenAnalyseState.LEFTPARENTHESESTATE;
                                break;
                            case ')':
                                state = TokenAnalyseState.RIGHTPARENTHESESTATE;
                                break;
                            case ';':
                                state = TokenAnalyseState.SEMICOLONSTATE;
                                break;
                            case '\'':
                                state = TokenAnalyseState.CHARVALSTATE;
                                break;
                            case '\"':
                                state = TokenAnalyseState.STRINGVALSTATE;
                                break;
                            case ',':
                                state = TokenAnalyseState.COMMASTATE;
                                break;
                            case ':':
                                state = TokenAnalyseState.COLONSTATE;
                                break;
                            default:
                                invalid = true;
                                break;
                        }
                    }
                    if (state != TokenAnalyseState.INITSTATE)
                        posToken = getPrevious();
                    if (invalid) {
                        unReadChar();
                        return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "错误的输入")));
                    }
                    if (state != TokenAnalyseState.INITSTATE && state != TokenAnalyseState.STRINGVALSTATE && state != TokenAnalyseState.CHARVALSTATE)
                        tkStr.append(nextChar);
                    break;
                }

                /*
                    零状态，可能会切换到
                    2. 可能就是一个0
                    3. 浮点数状态
                 */

                case ZEROSTATE: {
                    char ch = nextChar;
                    if (ch == '.' || ch == 'e' || ch == 'E') {
                        tkStr.append(ch);
                        state = TokenAnalyseState.DOUBLEVALSTATE;
                    }
                    else if (Util.isDigit(ch))
                        tkStr.append(ch);
                    else {
                        unReadChar();
                        if (tkStr.toString().equals("0"))
                            return new Pair<>(Optional.of(new Token(TokenKind.INT_LITERAL, 0, posToken.getRow(), posToken.getCol())), Optional.empty());
                        else
                            return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "无法识别的Token")));
                    }
                    break;
                }

                /*
                    整数状态，可能会转化到以下状态
                    1. 浮点数字面量状态
                    2. 不变，还是整数状态
                 */

                case DIGITSTATE: {
                    int re = -1;
                    if (isEOF()) {
                        try {
                            re = Integer.parseInt(input.toString());
                        } catch (NumberFormatException e) {
                        }
                        if (re != -1)
                            return new Pair<>(Optional.of(new Token(TokenKind.INT_LITERAL, re, posToken.getRow(), posToken.getCol())), Optional.empty());
                    }
                    boolean invalid = false;
                    if (Util.isDigit(nextChar))
                        tkStr.append(nextChar);
                    else if (nextChar == '.' || nextChar == 'E' || nextChar == 'e') {
                        tkStr.append(nextChar);
                        state = TokenAnalyseState.DOUBLEVALSTATE;
                    } else {
                        unReadChar();
                        try {
                            re = Integer.parseInt(tkStr.toString());
                        } catch (NumberFormatException e) {
                            return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "整数字面量溢出")));
                        }
                        if (re != -1)
                            return new Pair<>(Optional.of(new Token(TokenKind.INT_LITERAL, re, posToken.getRow(), posToken.getCol())), Optional.empty());
                    }
                    break;
                }
                    /*
                        标识符状态
                     */
                case IDENTIFERSTATE: {
                    if (isEOF())
                        return new Pair<>(Optional.of(new Token(getTokenKind(tkStr.toString()), tkStr.toString(), posToken.getRow(), posToken.getCol())), Optional.empty());
                    else if (Util.isDigit(nextChar) || Util.isAlpha(nextChar) || nextChar == '_')
                        tkStr.append(nextChar);
                    else {
                        unReadChar();
                        return new Pair<>(Optional.of(new Token(getTokenKind(tkStr.toString()), tkStr.toString(), posToken.getRow(), posToken.getCol())), Optional.empty());
                    }
                    break;
                }
                /*
                    加减乘除，除法需要额外处理
                 */
                case ADDSIGNSTATE: {
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.PLUS, "+", posToken.getRow(), posToken.getCol())), Optional.empty());
                }
                /*
                两种情况：- 和->
                 */
                case MINUSSIGNSTATE: {
                    if(nextChar != '>'){
                        unReadChar();
                        return new Pair<>(Optional.of(new Token(TokenKind.MINUS, "-", posToken.getRow(), posToken.getCol())), Optional.empty());
                    }
                    return new Pair<>(Optional.of(new Token(TokenKind.ARROW, "->", posToken.getRow(), posToken.getCol())), Optional.empty());

                }
                case MULTSIGNSTATE: {
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.MUL, "*", posToken.getRow(), posToken.getCol())), Optional.empty());
                }
                /*
                    读到'/'有二种可能
                    1.真的是'/'
                    2.单行注释
                 */
                case DIVSIGNSTATE: {
                    if (nextChar == '/') {
                        nextLine();
                        state = TokenAnalyseState.INITSTATE;
                        tkStr = new StringBuilder();
                        break;
                    }
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.DIV, "/", posToken.getRow(), posToken.getCol())), Optional.empty());
                }

                /*
                    读到<或者>都有两种可能
                    1.后面有'='组成'>='或'<='
                    2.后面啥都没
                 */

                case SMALLERSTATE: {
                    if (nextChar == '=')
                        return new Pair<>(Optional.of(new Token(TokenKind.LE, "<=", posToken.getRow(), posToken.getCol())), Optional.empty());
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.LT, "<", posToken.getRow(), posToken.getCol())), Optional.empty());
                }

                case BIGGERSTATE: {
                    if (nextChar == '=')
                        return new Pair<>(Optional.of(new Token(TokenKind.GE, ">=", posToken.getRow(), posToken.getCol())), Optional.empty());
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.GT, ">", posToken.getRow(), posToken.getCol())), Optional.empty());
                }

                /*
                    '!'后面一定要加'='，c0的运算里没有取非
                 */

                case NOTSTATE: {
                    if (nextChar == '=')
                        return new Pair<>(Optional.of(new Token(TokenKind.NEQ, "!", posToken.getRow(), posToken.getCol())), Optional.empty());
                    return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "错误的Token")));
                }

                /*
                    有两种可能
                    1.是赋值'='
                    2.是相等'=='
                 */

                case ASSIGNSTATE: {
                    if (nextChar == '=')
                        return new Pair<>(Optional.of(new Token(TokenKind.EQUAL, "==", posToken.getRow(), posToken.getCol())), Optional.empty());
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.ASSIGN, "=", posToken.getRow(), posToken.getCol())), Optional.empty());
                }

                case LEFTBRACESSTATE: {
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.L_BRACE, "{", posToken.getRow(), posToken.getCol())), Optional.empty());
                }

                case RIGHTBRACESSTATE: {
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.R_BRACE, "}", posToken.getRow(), posToken.getCol())), Optional.empty());
                }

                case LEFTPARENTHESESTATE: {
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.L_PAREN, "(", posToken.getRow(), posToken.getCol())), Optional.empty());
                }

                case RIGHTPARENTHESESTATE: {
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.R_PAREN, ")", posToken.getRow(), posToken.getCol())), Optional.empty());
                }

                case SEMICOLONSTATE: {
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.SEMICOLON, ";", posToken.getRow(), posToken.getCol())), Optional.empty());
                }

                case COLONSTATE: {
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.COLON, ":", posToken.getRow(), posToken.getCol())), Optional.empty());
                }

                case CHARVALSTATE: {
                    //if (!Util.isCchar(nextChar) && !Util.isEscape(nextChar))
                    //return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "错误的字符字面量")));
                    if (escapeChar) {
                        escapeChar = false;
                        if (nextChar == 'n'){
                            nextChar = readChar();
                            if(nextChar == '\'')
                                return new Pair<>(Optional.of(new Token(TokenKind.INT_LITERAL, (int)'\n', posToken.getRow(), posToken.getCol())), Optional.empty());
                            else
                                return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "缺少'")));
                        }
                        else if (nextChar == 'r'){
                            nextChar = readChar();
                            if(nextChar == '\'')
                                return new Pair<>(Optional.of(new Token(TokenKind.INT_LITERAL, (int)'\r', posToken.getRow(), posToken.getCol())), Optional.empty());
                            else
                                return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "缺少'")));
                        }
                        else if (nextChar == 't'){
                            nextChar = readChar();
                            if(nextChar == '\'')
                                return new Pair<>(Optional.of(new Token(TokenKind.INT_LITERAL, (int)'\t', posToken.getRow(), posToken.getCol())), Optional.empty());
                            else
                                return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "缺少'")));
                        }
                        else if (nextChar == '\\'){
                            nextChar = readChar();
                            if(nextChar == '\'')
                                return new Pair<>(Optional.of(new Token(TokenKind.INT_LITERAL, (int)'\\', posToken.getRow(), posToken.getCol())), Optional.empty());
                            else
                                return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "缺少'")));
                        }
                        else if (nextChar == '\"'){
                            nextChar = readChar();
                            if(nextChar == '\'')
                                return new Pair<>(Optional.of(new Token(TokenKind.INT_LITERAL, (int)'\"', posToken.getRow(), posToken.getCol())), Optional.empty());
                            else
                                return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "缺少'")));
                        }
                        return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "错误的字符字面量")));
                    } else {
                        if (nextChar == '\\') {
                            escapeChar = true;
                        } else {
                            if (readChar() == '\'')
                                return new Pair<>(Optional.of(new Token(TokenKind.INT_LITERAL, (int)nextChar, posToken.getRow(), posToken.getCol())), Optional.empty());
                            else
                                return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "缺少'")));
                        }
                    }
                    break;
                }

                /*
                    和字符字面量很相似，不过要读多次不能直接return
                 */

                case STRINGVALSTATE: {
                    //if (!Util.isSchar(nextChar) && !Util.isEscape(nextChar))
                    //return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "错误的字符串字面量")));
                    if (escape) {
                        escape = false;
                        if (nextChar == 'n')
                            tkStr.append("\n");
                        else if (nextChar == 'r')
                            tkStr.append("\r");
                        else if (nextChar == 't')
                            tkStr.append("\t");
                        else if (nextChar == '\\')
                            tkStr.append("\\");
                        else if (nextChar == '\"')
                            tkStr.append("\"");
                        else if (nextChar == '\'')
                            tkStr.append("\'");
                        else
                            return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "错误的字符串字面量")));
                    } else {
                        if (nextChar == '\\')
                            escape = true;
                        if (nextChar == '\"')
                            return new Pair<>(Optional.of(new Token(TokenKind.STRING_LITERAL, tkStr.toString(), posToken.getRow(), posToken.getCol())), Optional.empty());
                        if (nextChar != '\\')
                            tkStr.append(nextChar);
                        if(nextChar == '\u0000')
                            return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "错误的字符串字面量")));
                    }
                    break;
                }

                /*
                    浮点数的实现，读到'.'的时候切换到这个状态
                    读到数字就继续读入
                    读到'e'或者'E'的时候开始计算浮点数的值
                    方法是分别计算前面的小数部分和后面的指数部分
                 */

                case DOUBLEVALSTATE: {
                    if (Util.isDigit(nextChar))
                        tkStr.append(nextChar);
                    else if (nextChar == 'e' || nextChar == 'E') {
                        double frac = -1;
                        try {
                            frac = Double.parseDouble(tkStr.toString());
                        } catch (NumberFormatException e) {
                            return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "浮点数溢出")));
                        }
                        char sign = readChar();
                        int flag = 1;
                        if (sign == '-')
                            flag = -1;
                        if (sign != '+' && sign != '-')
                            unReadChar();
                        // digit seq
                        StringBuilder exp = new StringBuilder();
                        int expVal = -1;
                        char ch = readChar();
                        while (Util.isDigit(ch)) {
                            exp.append(ch);
                            ch = readChar();
                        }
                        try {
                            expVal = Integer.parseInt(exp.toString());
                        } catch (NumberFormatException e) {
                            return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "整数字面量溢出")));
                        }
                        if (expVal != -1) {
                            for (int i = 0; i < expVal; i++)
                                frac *= 10;
                        }
                        return new Pair<>(Optional.of(new Token(TokenKind.DOUBLE_LITERAL, frac, posToken.getRow(), posToken.getCol())), Optional.empty());
                    } else {
                        if (tkStr.charAt(tkStr.length() - 1) == '.')
                            tkStr.append(0);
                        double frac = -1;
                        try {
                            frac = Double.parseDouble(tkStr.toString());
                        } catch (NumberFormatException e) {
                            return new Pair<>(Optional.empty(), Optional.of(new TokenError(posToken.getRow(), posToken.getCol(), "浮点数溢出")));
                        }
                        if (frac != -1)
                            return new Pair<>(Optional.of(new Token(TokenKind.DOUBLE_LITERAL, frac, posToken.getRow(), posToken.getCol())), Optional.empty());
                    }
                    break;
                }

                case COMMASTATE: {
                    unReadChar();
                    return new Pair<>(Optional.of(new Token(TokenKind.COMMA, ",", posToken.getRow(), posToken.getCol())), Optional.empty());
                }
            }
        }
    }


    private TokenKind getTokenKind(String kd) {
        switch (kd) {
            case "fn":
                return TokenKind.FN_KW;
            case "let":
                return TokenKind.LET_KW;
            case "const":
                return TokenKind.CONST_KW;
            case "as":
                return TokenKind.AS_KW;
            case "while":
                return TokenKind.WHILE_KW;
            case "if":
                return TokenKind.IF_KW;
            case "else":
                return TokenKind.ELSE_KW;
            case "return":
                return TokenKind.RETURN_KW;
            case "break":
                return TokenKind.BREAK_KW;
            case "continue":
                return TokenKind.CONTINUE_KW;
            case "int":
                return TokenKind.INT;
            case "void":
                return TokenKind.VOID;
            case "double":
                return TokenKind.DOUBLE;
            case "getint":
                return TokenKind.GETINT;
            case "getchar":
                return TokenKind.GETCHAR;
            case "putint":
                return TokenKind.PUTINT;
            case "putchar":
                return TokenKind.PUTCHAR;
            case "putstr":
                return TokenKind.PUTSTR;
            case "putln":
                return TokenKind.PUTLN;
            default:
                return TokenKind.IDENT;
        }
    }

    public TokenAnalyse(String pathName) {
        File file = new File(pathName);
        InputStreamReader reader;
        BufferedReader br = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file));
            br = new BufferedReader(reader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            String in = br.readLine();
            while (in != null) {
                this.input.add(in + "\n");
                in = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void nextLine() {
        if (isEOF())
            return;
        if (pos.getRow() == input.size() - 1)
            return;
        else {
            pos.setRow(pos.getRow() + 1);
            pos.setCol(0);
        }
    }

    public char readChar() {
        if (isEOF())
            return '\u0000';
        char re = input.get(pos.getRow()).charAt(pos.getCol());
        if (pos.getCol() == input.get(pos.getRow()).length() - 1) {
            pos.setRow(pos.getRow() + 1);
            pos.setCol(0);
        } else
            pos.setCol(pos.getCol() + 1);

        return re;
    }

    public void unReadChar() {
        try {
            if (pos.getRow() == 0 && pos.getCol() == 0)
                throw new IndexOutOfBoundsException("已经是第一个字符，无法回退");
            else if (pos.getCol() == 0) {
                pos.setRow(pos.getRow() - 1);
                pos.setCol(input.get(pos.getRow()).length() - 1);
            } else
                pos.setCol(pos.getCol() - 1);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    private boolean isEOF() {
        return (pos.getRow() == input.size() && pos.getCol() == 0);
    }

    public Pos getPrevious() {
        try {
            if (pos.getRow() == 0 && pos.getCol() == 0)
                throw new IndexOutOfBoundsException("已是最开始了，无法再回读");
            if (pos.getCol() == 0)
                return new Pos(pos.getRow() - 1, input.get(pos.getRow() - 1).length() - 1);
            return new Pos(pos.getRow(), pos.getCol() - 1);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return null;
    }

}
