package com.company.GrammerAnalyse;

import com.company.Instruction.Instruction;
import com.company.Instruction.InstructionType;
import com.company.SymbolTable.*;
import com.company.TokenAnalyse.Token;
import com.company.TokenAnalyse.TokenAnalyse;
import com.company.TokenAnalyse.TokenKind;
import com.company.Util.Pair;
import com.company.error.GrammerError.GrammerError;
import com.company.error.TokenError.ErrEOF;
import com.company.error.TokenError.TokenError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class GrammerAnalyse {
    private ArrayList<Token> tkList = new ArrayList<>();
    private int tokenIndex = 0;
    private SymbolIndexTable table = new SymbolIndexTable();
    private HashMap<TokenKind, ValType> tokenKindSymbolTypeHashMap = new HashMap<>();
    private Instruction ins = new Instruction();

    {
        tokenKindSymbolTypeHashMap.put(TokenKind.INT, ValType.INTEGER);
    }

    private globalFunctions context = new globalFunctions(0, 0, 0, 0, null, 0, null, null);

    public void DoGrammerAnalyse(String pathname, String out) throws IOException {
        TokenAnalyse tokenAnalyse = new TokenAnalyse(pathname);
        Pair<Optional<Token>, Optional<Exception>> tk = tokenAnalyse.NextToken();
        while (!tk.second.isPresent()) {
            tkList.add(tk.first.get());
            //System.out.println(tk.first.get().getTokenKind() + " " + tk.first.get().getValStr() + " " + tk.first.get().getValInt());
            tk = tokenAnalyse.NextToken();
            if (tk.second.isPresent())
                break;
        }
        if (tk.second.get().getClass().getSimpleName().equals("ErrEOF")) {
            Token eof = new Token();
            eof.setEOF(true);
            tkList.add(eof);
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.NOP, 0, 0);
            Optional<Exception> err = AnalyseProgram();
            if (err.isPresent()) {
                try {
                    throw err.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
            ins.generateCode(table, out);
            System.out.println("编译成功!");
        } else {
            try {
                throw tk.second.get();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private Optional<Exception> AnalyseProgram() {
        Optional<Exception> err;
        err = AnalyseVariableDeclaration().first;
        if (err.isPresent())
            return err;
        err = AnalyseFunctionDefinition();
        boolean hasMain = false;
        for (globalFunctions f : table.getGblFuncTable()){
            if (f.getName().equals("main")){
                hasMain = true;
                break;
            }
        }
        if (!hasMain)
            return Optional.of(new GrammerError(0, 0, "缺少main函数"));
        return err;
    }

         /*
        变量和常量声明，要注意
        1.常量的值必须要被显式初始化
        2.常量的值后面不能被修改
        3.不能重复声明
     */


    private Pair<Optional<Exception>, Integer> AnalyseVariableDeclaration() {
        int count = 0;
        while (true) {
            boolean isConstant = false;
            // 预读，看是不是const
            Token next = NextToken().first.get();
            if (next.isEOF())
                return new Pair<>(Optional.of(new ErrEOF(next.getRowNum(), next.getColNum(), "EOF")), 0);
            else if (next.getTokenKind() == TokenKind.CONST_KW)
                isConstant = true;
            else if (next.getTokenKind() != TokenKind.LET_KW){
                UnreadToken();
                return new Pair<>(Optional.empty(), count);
            }
            while (true) {
                // 读取identifer
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.IDENT)
                    return new Pair<>(Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "变量声明缺少标识符")), 0);
                String tokenName = next.getValStr();
                // 不能重复声明
                if (table.isDuplicate(next.getValStr()))
                    return new Pair<>(Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "重复定义")), 0);
                //检查:
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.COLON)
                    return new Pair<>(Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少冒号")),0);
                //读变量类型
                next = NextToken().first.get();
                if(next.getTokenKind() != TokenKind.DOUBLE && next.getTokenKind() != TokenKind.INT)
                    return new Pair<>(Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少变量类型")), 0);
                TokenKind orgType = next.getTokenKind();
                // 预读，看看是否是'='，如果是变量可以没有，如果是常量必须得赋值
                next = NextToken().first.get();
                if (isConstant) {
                    if (next.getTokenKind() != TokenKind.ASSIGN)
                        return new Pair<>(Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "常量要赋值")), 0);
                    // 如果是'='，分析表达式，并将结果填入符号表
                    ++count;
                    transInsert(tokenName, orgType, true, count);
                    next = NextToken().first.get();
                    if (next.getTokenKind() == TokenKind.SEMICOLON)
                        break;
                    return new Pair<>(Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "错误的声明")), 0);
                } else {
                    // 未赋值的情况
                    if (next.getTokenKind() != TokenKind.ASSIGN) {
                        // 未赋值赋初值0
                        //ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PUSH, 0, 0);
                        if (next.getTokenKind() == TokenKind.SEMICOLON) {
                            ++count;
                            if(table.getCurrentLevel() == 0)
                                table.insertGlobalConst(tokenName, 'I', "0", false);
                            table.insertSymbol(tokenName, new SymbolEntry(tokenName, 0, SymbolType.VARIABLE, table.getCurrentLevel(), tokenKindSymbolTypeHashMap.get(orgType), count));
                            int level = table.getCurrentLevel();
                            if (level == 0){
                                //全局变量
                                int offset = table.getGlobalConst(tokenName).getIndex();
                                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.GLOBA, offset, 0);
                            }
                            else {
                                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.LOCA, (long)table.findSymbol(tokenName).getOffset(), 0);
                            }
                            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PUSH, 0, 0);
                            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.STORE64, 0, 0);
                            break;
                        }
                        return new Pair<>(Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "错误的声明")), 0);
                    } else {
                        // 赋值的情况
                        transInsert(tokenName, orgType, false, count);
                        next = NextToken().first.get();
                        if (next.getTokenKind() == TokenKind.SEMICOLON){
                            ++count;
                            break;
                        }
                        return new Pair<>(Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "错误的声明")), 0);
                    }
                }

            }
        }
    }

    private Optional<Exception> AnalyseIfStatement() {
        // 读入if
        boolean hasParam = false;
        Token next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.IF_KW)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少if"));
        // 读入(
        next = NextToken().first.get();
        if (next.getTokenKind() == TokenKind.L_PAREN)
            hasParam = true;
        else
            UnreadToken();
        Pair<Optional<Integer>, Optional<Exception>> err = AnalyseConditionalStatement();
        if (err.second.isPresent())
            return Optional.of(err.second.get());
        int falseJmpAddr = err.first.get();
        next = NextToken().first.get();
        if (hasParam){
            if (next.getTokenKind() != TokenKind.R_PAREN)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少)"));
        }
        else{
            if (next.getTokenKind() == TokenKind.R_PAREN)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少("));
            UnreadToken();
        }
        next = NextToken().first.get();
        if(next.getTokenKind() != TokenKind.L_BRACE)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "不是代码块"));
        UnreadToken();
        Pair<Optional<Boolean>, Optional<Exception>> err1 = AnalyseStatement();
        if (err1.second.isPresent())
            return err1.second;
        // 获取跳转地址
        int trueJmpAddr = ins.getNextCodeOffset() - falseJmpAddr;
        int noCon = ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.BR, 0, 0);
        int finalJmpAddr = -1;
        // 看是否是else
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.ELSE_KW) {
            UnreadToken();
            finalJmpAddr = ins.getCodeOffset() - noCon;
        } else {
            err1 = AnalyseStatement();
            if (err1.second.isPresent())
                return err1.second;
            finalJmpAddr = ins.getCodeOffset() - noCon;
        }
        ins.updateInstruction(noCon, finalJmpAddr, 0);
        ins.updateInstruction(falseJmpAddr, trueJmpAddr, 0);
        return Optional.empty();
    }

    private Pair<Optional<Integer>, Optional<Exception>> AnalyseConditionalStatement() {
        Optional<Exception> err = AnalyseExpression();
        int falseJpc = -1;
        if (err.isPresent())
            return new Pair<>(Optional.of(-1), Optional.of(err.get()));
        // 判断条件表达式的符号
        TokenKind type;
        Token next = NextToken().first.get();
        switch (next.getTokenKind()) {
            case LT:   // <
            case GT:    // >
            case GE:    // >=
            case LE:     // <=
            case EQUAL:     // ==
            case NEQ: // !=
                type = next.getTokenKind();
                break;
            case R_PAREN:
            case L_BRACE:
                // 直接是表达式，判断是不是0
                UnreadToken();
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PUSH, 0, 0);
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.CMPI, 0, 0);
                falseJpc = ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.BRFALSE, 0, 0);
                return new Pair<>(Optional.of(falseJpc), Optional.empty());
            default:
                return new Pair<>(Optional.of(-1), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "错误的条件表达式")));
        }
        err = AnalyseExpression();
        if (err.isPresent())
            return new Pair<>(Optional.of(-1), Optional.of(err.get()));
        ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.CMPI, 0, 0);
        switch (type){
            case EQUAL:
                falseJpc = ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.BRTRUE, 0, 0);
                break;
            case NEQ:
                falseJpc = ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.BRFALSE, 0, 0);
                break;
            case GT:
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.SETGT, 0, 0);
                falseJpc = ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.BRFALSE, 0, 0);
                break;
            case LT:
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.SETLT, 0, 0);
                falseJpc = ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.BRFALSE, 0, 0);
                break;
            case GE:
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.SETLT, 0, 0);
                falseJpc = ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.BRTRUE, 0, 0);
                break;
            case LE:
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.SETGT, 0, 0);
                falseJpc = ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.BRTRUE, 0, 0);
                break;
            default:
                return new Pair<>(Optional.of(-1), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "错误的条件表达式")));
        }
        return new Pair<>(Optional.of(falseJpc), Optional.empty());
    }

    private Optional<Exception> AnalyseWhileStatement() {
        // 读入while
        Token next = NextToken().first.get();
        boolean hasParam = false;
        if (next.getTokenKind() != TokenKind.WHILE_KW)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少while"));
        // 读入'('
        next = NextToken().first.get();
        if (next.getTokenKind() == TokenKind.L_PAREN)
            hasParam = true;
        else
            UnreadToken();
        // 循环开始时地址
        //int startAddr = ins.getCodeOffset();
        Pair<Optional<Integer>, Optional<Exception>> err = AnalyseConditionalStatement();
        if (err.second.isPresent())
            return Optional.of(err.second.get());
        // 得出的假地址的index，便于之后回填
        int falseJmpAddr = err.first.get();
        next = NextToken().first.get();
        if (hasParam){
            if (next.getTokenKind() != TokenKind.R_PAREN)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少)"));
        }
        else{
            if (next.getTokenKind() == TokenKind.R_PAREN)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少("));
            UnreadToken();
        }
        next = NextToken().first.get();
        if(next.getTokenKind() != TokenKind.L_BRACE)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "不是代码块"));
        UnreadToken();
        Pair<Optional<Boolean>, Optional<Exception>> err1 = AnalyseStatement();
        if (err1.second.isPresent())
            return err1.second;
        // 无条件跳回循环开始处
        //ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.JMP, startAddr, 0);
        //int finalAddr = ins.getCodeOffset();
        //ins.updateInstruction(falseJmpAddr, finalAddr, 0);
        // 检查continue和break
        //ins.updateConBre(startAddr, finalAddr);
        return Optional.empty();
    }

    private Optional<Exception> AnalyseCallStatement() {
        // 读入函数名
        Token next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.IDENT)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少函数名"));
        globalFunctions func = table.getGlobalFunc(next.getValStr());
        if (func == null)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "此函数未定义"));
        // 读入'('
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.L_PAREN)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少("));
        if (func.getRtnType() != RtnType.VOID)
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.STACKALLOC, 1L, 0);
        int argc = 0;
        // 预读,可能是')'或者传的实参值
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.R_PAREN) {
            UnreadToken();
            while (true) {
                Optional<Exception> err = AnalyseExpression();
                if (err.isPresent())
                    return Optional.of(err.get());
                next = NextToken().first.get();
                if (next.getTokenKind() == TokenKind.R_PAREN) {
                    argc++;
                    break;
                } else if (next.getTokenKind() == TokenKind.COMMA) {
                    argc++;
                    continue;
                }
            }
        }
        if (argc > func.getParams_size())
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "参数错误,输入参数过多"));
        else if (argc < func.getParams_size())
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "参数错误,输入参数过少"));
        else {
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.CALL, (long)func.getIndex(), 0);
            return Optional.empty();
        }

    }

    private Optional<Exception> AnalyseAssignStatement() {
        // 读入identifer
        Token next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.IDENT)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "声明时缺少标识符"));
        // 要已经声明过，并且不能是函数或者常量
        SymbolEntry find = table.findSymbol(next.getValStr());
        if (find == null)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "标识符未定义,无法赋值"));
        if (find.getType() == SymbolType.FUNCTION || find.getType() == SymbolType.CONSTANT || find.getType() == SymbolType.CONSTARGUMENTS)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "函数和常量无法赋值"));
        // 先把标识符的地址找到，再把表达式的值赋给它
        int level = find.getLevel();
        //int offset = getOffset(find);
        // 从符号表中获取层级和偏移，生成load指令获取地址
        if (level == 0){
            //全局变量
            int offset = table.getGlobalConst(find.getName()).getIndex();
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.GLOBA, (long)offset, 0);
        }
        else {
            //函数参数和局部变量
            if (find.getType() == SymbolType.ARGUMENTS || find.getType() == SymbolType.CONSTARGUMENTS){
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.ARGA, (long)find.getOffset(), 0);
            }
            else{
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.LOCA, (long)find.getOffset(), 0);
            }
        }
        // 读入赋值号
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.ASSIGN)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "赋值缺少'='"));
        Optional<Exception> err = AnalyseExpression();
        if (err.isPresent())
            return Optional.of(err.get());
        ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.STORE64, 0, 0);
        return Optional.empty();
    }

    private Optional<Exception> AnalyseReturnStatement() {
        // 读入return
        Token next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.RETURN_KW)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少return"));
        // 看下一个是不是;
        next = NextToken().first.get();
        if (next.getTokenKind() == TokenKind.SEMICOLON) {
            UnreadToken();
            if (context.getRtnType() != RtnType.VOID)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "应该有返回值"));
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.RET, 0, 0);
        } else {
            UnreadToken();
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.ARGA, 0, 0);
            // 表达式
            Optional<Exception> err = AnalyseExpression();
            if (err.isPresent())
                return Optional.of(err.get());
            if (context.getRtnType() == RtnType.VOID)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "返回类型不能为空"));
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.STORE64, 0, 0);
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.RET, 0, 0);
        }
        return Optional.empty();
    }



    // 看返回的GrammerError中stop是否是true判断是否结束语句
    private Pair<Optional<Boolean>, Optional<Exception>> AnalyseStatement() {
        // 预读
        Token next = NextToken().first.get();
        TokenKind type = next.getTokenKind();
        Optional<Exception> err = null;
        switch (type) {
            case IF_KW:
                UnreadToken();
                err = AnalyseIfStatement();
                if (err.isPresent())
                    return new Pair<>(Optional.of(false), Optional.of(err.get()));
                break;
            case WHILE_KW:
                UnreadToken();
                err = AnalyseWhileStatement();
                if (err.isPresent())
                    return new Pair<>(Optional.of(false), Optional.of(err.get()));
                break;
            case RETURN_KW:
                UnreadToken();
                err = AnalyseReturnStatement();
                if (err.isPresent())
                    return new Pair<>(Optional.of(false), Optional.of(err.get()));
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.SEMICOLON)
                    return new Pair<>(Optional.of(false), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少分号")));
                break;
            case L_BRACE:
                table.nextLevel();
                err = AnalyseVariableDeclaration().first;
                if (err.isPresent())
                    return new Pair<>(Optional.of(false), Optional.of(err.get()));
                err = AnalyseStatementSequence();
                if (err.isPresent())
                    return new Pair<>(Optional.of(false), Optional.of(err.get()));
                // 看是否是右括号
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.R_BRACE)
                    return new Pair<>(Optional.of(false), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少}")));
                // 弹出所有的局部变量
                //for (int i = 0; i < table.getOffset(); i++)
                    //ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.POP, 0, 0);
                // 回退层次，删除符号表
                try {
                    table.previousLevel();
                } catch (GrammerError e) {
                    e.printStackTrace();
                }
                break;
            case IDENT:
                boolean isFunc = false;
                // 预读，判断是否是函数
                Token pre = NextToken().first.get();
                if (pre.getTokenKind() == TokenKind.L_PAREN)
                    isFunc = true;
                if (isFunc) {
                    globalFunctions fun = table.getGlobalFunc(next.getValStr());
                    UnreadToken();
                    UnreadToken();
                    err = AnalyseCallStatement();
                    if (err.isPresent())
                        return new Pair<>(Optional.of(false), Optional.of(err.get()));
                    // 不是空的时候pop一下
                    //if (fun.getRtnType() != RtnType.VOID)
                        //ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.POP, 0, 0);
                    // 检查';'
                    next = NextToken().first.get();
                    if (next.getTokenKind() != TokenKind.SEMICOLON)
                        return new Pair<>(Optional.of(false), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少分号")));
                } else {
                    UnreadToken();
                    SymbolEntry find = table.findSymbol(next.getValStr());
                    if (find.getType() == SymbolType.ARGUMENTS || find.getType() == SymbolType.VARIABLE) {
                        UnreadToken();
                        err = AnalyseAssignStatement();
                        if (err.isPresent())
                            return new Pair<>(Optional.of(false), Optional.of(err.get()));
                        next = NextToken().first.get();
                        if (next.getTokenKind() != TokenKind.SEMICOLON)
                            return new Pair<>(Optional.of(false), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少分号")));
                    } else
                        return new Pair<>(Optional.of(false), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "不能给常量赋值")));
                }
                break;
            case CONTINUE_KW:
                // 设定jmp后两个参数都为-6，回填的时候知道这句话是continue
                //ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.JMP, -6, -6);
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.SEMICOLON)
                    return new Pair<>(Optional.of(false), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少分号")));
                break;
            case BREAK_KW:
                // 设定jmp后两个参数都为-9，回填时就知道这句话是break
                //ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.JMP, -9, -9);
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.SEMICOLON)
                    return new Pair<>(Optional.of(false), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少分号")));
                break;
            case SEMICOLON:
                break;
            case PUTCHAR:
                UnreadToken();
                err = AnalysePutCharStatement();
                if (err.isPresent())
                    return new Pair<>(Optional.of(false), Optional.of(err.get()));
                break;
            case PUTINT:
                UnreadToken();
                err = AnalysePutIntStatement();
                if (err.isPresent())
                    return new Pair<>(Optional.of(false), Optional.of(err.get()));
                break;
            case PUTSTR:
                UnreadToken();
                err = AnalysePutStrStatement();
                if (err.isPresent())
                    return new Pair<>(Optional.of(false), Optional.of(err.get()));
                break;
            case PUTLN:
                UnreadToken();
                err = AnalysePutLnStatement();
                if (err.isPresent())
                    return new Pair<>(Optional.of(false), Optional.of(err.get()));
                break;
            default:
                UnreadToken();
                return new Pair<>(Optional.of(true), Optional.empty());
        }
        return new Pair<>(Optional.of(false), Optional.empty());
    }

    private Optional<Exception> AnalysePutIntStatement() {
        // 读入putint
        Token next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.PUTINT)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少putint"));
        // 读入'('
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.L_PAREN)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少("));
            //读入int
        next = NextToken().first.get();
        if (next.getTokenKind() == TokenKind.INT_LITERAL){
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PUSH, (long)next.getValInt(), 0);
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PRINTI, 0, 0);
        }
        else{
            UnreadToken();
            Optional<Exception> err = AnalyseExpression();
            if (err.isPresent())
                return Optional.of(err.get());
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PRINTI, 0, 0);
        }
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.R_PAREN){
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少)"));
        }
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.SEMICOLON)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少分号"));
        //ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PRINTL, 0, 0);
        return Optional.empty();
    }

    private Optional<Exception> AnalysePutCharStatement() {
        // 读入putchar
        Token next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.PUTCHAR)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少putchar"));
        // 读入'('
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.L_PAREN)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少("));
        //读入int
        next = NextToken().first.get();
        if (next.getTokenKind() == TokenKind.INT_LITERAL){
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PUSH, (long)next.getValInt(), 0);
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PRINTC, 0, 0);
        }
        else{
            UnreadToken();
            Optional<Exception> err = AnalyseExpression();
            if (err.isPresent())
                return Optional.of(err.get());
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PRINTC, 0, 0);
        }
        next = NextToken().first.get();
        if(next.getTokenKind() != TokenKind.R_PAREN)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少）"));
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.SEMICOLON)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少分号"));
        return Optional.empty();
    }

    private Optional<Exception> AnalysePutStrStatement() {
        // 读入putstr
        Token next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.PUTSTR)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少putstr"));
        // 读入'('
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.L_PAREN)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少("));
        //读入int
        next = NextToken().first.get();
        if (next.getTokenKind() == TokenKind.INT_LITERAL){
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PUSH, (long)next.getValInt(), 0);
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PRINTS, 0, 0);
        }
        else if (next.getTokenKind() == TokenKind.STRING_LITERAL) {
            String val = next.getValStr();
            if (table.getGlobalConst(val) == null)
                table.insertGlobalConst(val, 'S', val, true);
            globalConstants con = table.getGlobalConst(val);
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PUSH, (long)con.getIndex(), 0);
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PRINTS, 0, 0);
        }
        else{
            UnreadToken();
            Optional<Exception> err = AnalyseExpression();
            if (err.isPresent())
                return Optional.of(err.get());
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PRINTS, 0, 0);
        }
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.R_PAREN)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少)"));
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.SEMICOLON)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少分号"));
        //ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PRINTL, 0, 0);
        return Optional.empty();
    }

    private Optional<Exception> AnalysePutLnStatement() {
        // 读入putln
        Token next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.PUTLN)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少putln"));
        // 读入'('
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.L_PAREN)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少("));
        // 读入')'
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.R_PAREN)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少)"));
        next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.SEMICOLON)
            return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少分号"));
        ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PRINTLN, 0, 0);
        return Optional.empty();
    }

    private Optional<Exception> AnalyseStatementSequence() {
        while (true) {
            Pair<Optional<Boolean>, Optional<Exception>> err = AnalyseStatement();
            if (err.second.isPresent())
                return Optional.of(err.second.get());
            else if (err.first.isPresent() && err.first.get())
                break;
        }
        return Optional.empty();
    }

    private Optional<Exception> AnalyseFactor() {
        // 预定义返回值
        int val = 0;
        // 预读，看是不是符号
        Token next = NextToken().first.get();
        int pre = 1;
        if (next.getTokenKind() != TokenKind.PLUS && next.getTokenKind() != TokenKind.MINUS)
            UnreadToken();
        else if (next.getTokenKind() == TokenKind.MINUS)
            pre = -1;
        next = NextToken().first.get();
        switch (next.getTokenKind()) {
            case IDENT: {
                // 首先看这个标识符是否定义了
                // 要把这个标识符的值给LIT到栈顶
                Token org = next;
                next = NextToken().first.get();
                if (next.getTokenKind() == TokenKind.L_PAREN) {
                    UnreadToken();
                    globalFunctions func = table.getGlobalFunc(org.getValStr());
                    if (func == null)
                        return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "非法的函数调用"));
                    else {
                        UnreadToken();
                        Optional<Exception> err = AnalyseCallStatement();
                        if (err.isPresent())
                            return err;
                    }
                } else {
                    UnreadToken();
                    SymbolEntry entry = table.findSymbol(org.getValStr());
                    if (entry == null)
                        return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "标识符未定义"));
                        // 生成指令，判断是常量还是变量分别处理
                    else {
                        int level = entry.getLevel();
                        // 从符号表中获取层级和偏移，生成load指令获取地址
                        if (level == 0){
                            //全局变量
                            int offset = table.getGlobalConst(entry.getName()).getIndex();
                            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.GLOBA, (long)offset, 0);
                        }
                        else {
                            //函数参数和局部变量
                            if (entry.getType() == SymbolType.ARGUMENTS || entry.getType() == SymbolType.CONSTARGUMENTS){
                                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.ARGA, (long)entry.getOffset(), 0);
                            }
                            else{
                                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.LOCA, (long)entry.getOffset(), 0);
                            }
                        }
                        ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.LOAD64, 0, 0);
                    }
                }
                break;
            }
            case GETINT:
                // 读入'('
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.L_PAREN)
                    return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "getint缺左括号"));
                // 读入')'
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.R_PAREN)
                    return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少)"));
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.SCANI,0, 0);
                break;
            case GETCHAR:
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.L_PAREN)
                    return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "getchar缺左括号"));
                // 读入')'
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.R_PAREN)
                    return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少)"));
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.SCANC,0, 0);
                break;
            case L_PAREN:
                Optional<Exception> err = AnalyseExpression();
                if (err.isPresent())
                    return err;
                next = NextToken().first.get();
                if (next.getTokenKind() != TokenKind.R_PAREN)
                    return Optional.of(new TokenError(next.getRowNum(), next.getColNum(), "括号不匹配,缺少')'"));
                break;
            case INT_LITERAL:
                val = next.getValInt();
                // 入栈常量
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PUSH, (long)pre * val, 0);
                break;
            default:
                return Optional.of(new TokenError(next.getRowNum(), next.getColNum(), "因子分析错误"));
        }
        return Optional.empty();
    }

    private Optional<Exception> AnalyseItem() {
        Optional<Exception> err = AnalyseFactor();
        if (err.isPresent())
            return err;
        while (true) {
            Token next = NextToken().first.get();
            if (next.isEOF())
                return Optional.empty();
            if (next.getTokenKind() != TokenKind.MUL && next.getTokenKind() != TokenKind.DIV) {
                UnreadToken();
                return Optional.empty();
            }
            InstructionType type;
            if (next.getTokenKind() == TokenKind.MUL)
                type = InstructionType.MULI;
            else
                type = InstructionType.DIVI;
            // <因子>
            err = AnalyseFactor();
            if (err.isPresent())
                return err;
            // 生成乘法或除法指令
            ins.pushBackInstruction(ins.getCodeOffset(), type, 0, 0);
        }
    }

    private Pair<Optional<ArrayList<ArgsInfo>>, Optional<Exception>> AnalyseArguments() {
        int index = 1;
        // 读入'('
        ArrayList<ArgsInfo> args = new ArrayList<>();
        Token next = NextToken().first.get();
        if (next.getTokenKind() != TokenKind.L_PAREN)
            return new Pair<>(Optional.empty(), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "错误的参数表,缺少'('")));
        // 预读，看看是不是')'
        next = NextToken().first.get();
        if (next.getTokenKind() == TokenKind.R_PAREN)
            return new Pair<>(Optional.of(new ArrayList<>()), Optional.empty());
        UnreadToken();
        while (true) {
            // 预读，看是否是常量
            boolean isConstant = false;
            next = NextToken().first.get();
            if (next.getTokenKind() == TokenKind.CONST_KW)
                isConstant = true;
            else
                UnreadToken();
            // 读入形参变量
            next = NextToken().first.get();
            if (next.getTokenKind() != TokenKind.IDENT)
                return new Pair<>(Optional.empty(), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少标识符")));
            // 防止参数中有重复的
            if (table.isDuplicate(next.getValStr()))
                return new Pair<>(Optional.empty(), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "重复定义")));
            String identName = next.getValStr();
            //检查':'
            next = NextToken().first.get();
            if (next.getTokenKind() != TokenKind.COLON)
                return new Pair<>(Optional.empty(), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少':'")));
            // 读取类型
            next = NextToken().first.get();
            if (next.getTokenKind() != TokenKind.DOUBLE && next.getTokenKind() != TokenKind.INT)
                return new Pair<>(Optional.empty(), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "错误的参数类型")));
            // 暂存类型
            TokenKind type = next.getTokenKind();
            ValType valType;
            switch (type) {
                case DOUBLE:
                    valType = ValType.DOUBLE;
                    break;
                case INT:
                    valType = ValType.INTEGER;
                    break;
                default:
                    return new Pair<>(Optional.empty(), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "未知错误")));
            }
            ArgsInfo argsInfo = new ArgsInfo(isConstant, valType, identName, index);
            args.add(argsInfo);
            // 参数插入符号表，注意函数的参数可能是常量，需要保存
            if (isConstant)
                table.insertSymbol(identName, new SymbolEntry(identName, 0, SymbolType.CONSTARGUMENTS, table.getCurrentLevel(), tokenKindSymbolTypeHashMap.get(type), index));
            else
                table.insertSymbol(identName, new SymbolEntry(identName, 0, SymbolType.ARGUMENTS, table.getCurrentLevel(), tokenKindSymbolTypeHashMap.get(type), index));
            ++index;
            // 看是右括号还是','
            next = NextToken().first.get();
            if (next.getTokenKind() == TokenKind.R_PAREN) {
                return new Pair<>(Optional.of(args), Optional.empty());
            } else if (next.getTokenKind() == TokenKind.COMMA)
                continue;
            else
                return new Pair<>(Optional.empty(), Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "错误的参数声明")));
        }
    }

    // <表达式> ::= <项>{<加法型运算符><项>}
    private Optional<Exception> AnalyseExpression() {
        Optional<Exception> err = AnalyseItem();
        if (err.isPresent())
            return err;
        while (true) {
            Token next = NextToken().first.get();
            if (next.isEOF())
                return Optional.empty();
            if (next.getTokenKind() != TokenKind.PLUS && next.getTokenKind() != TokenKind.MINUS) {
                UnreadToken();
                return Optional.empty();
            }
            InstructionType type;
            if (next.getTokenKind() == TokenKind.PLUS)
               type = InstructionType.ADDI;
            else
                type = InstructionType.SUBI;
            err = AnalyseItem();
            if (err.isPresent())
                return err;
            ins.pushBackInstruction(ins.getCodeOffset(), type, 0, 0);
        }
    }

    private Optional<Exception> AnalyseFunctionDefinition() {
        while (true) {
            ins.clearOffset();
            // 读入函数返回类型
            Token next = NextToken().first.get();
            if (next.isEOF())
                return Optional.empty();
            if (next.getTokenKind() != TokenKind.FN_KW)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少FN关键字"));
            next = NextToken().first.get();
            // 读入函数名
            if (next.getTokenKind() != TokenKind.IDENT)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少函数名"));
            String funcName = next.getValStr();
            table.insertGlobalConst(funcName, 'S', funcName, true);
            // 不能重复定义函数
            if (table.isDuplicate(funcName))
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "重复的定义"));
            // 进入下一层
            table.nextLevel();
            // 分析参数
            Pair<Optional<ArrayList<ArgsInfo>>, Optional<Exception>> arg = AnalyseArguments();
            if (arg.second.isPresent())
                return Optional.of(arg.second.get());
            next = NextToken().first.get();
            if (next.getTokenKind() != TokenKind.ARROW)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少->"));
            next = NextToken().first.get();
            if (next.getTokenKind() != TokenKind.DOUBLE && next.getTokenKind() != TokenKind.INT && next.getTokenKind() != TokenKind.VOID)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "缺少类型信息"));
            TokenKind rtnType = next.getTokenKind();
            RtnType rtn_type = null;
            switch (rtnType) {
                case INT:
                    rtn_type = RtnType.INT;
                    break;
                case VOID:
                    rtn_type = RtnType.VOID;
                    break;
                default:
                    return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "未知错误"));
            }
            // 函数体
            next = NextToken().first.get();
            if (next.getTokenKind() != TokenKind.L_BRACE)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "函数体不是代码块"));
            // variable-declaration
            Pair<Optional<Exception>, Integer> a = AnalyseVariableDeclaration();
            Optional<Exception> re = a.first;
            if (re.isPresent())
                return re;
            // 分析局部变量
            table.insertGlobalFunc(funcName, arg.first.get().size(), table.getCurrentLevel(), arg.first.get(), 0, rtn_type);
            context = table.getGlobalFunc(funcName);
            context.setCount(a.second);
            re = AnalyseStatementSequence();
            if (re.isPresent())
                return re;
            next = NextToken().first.get();
            if (next.getTokenKind() != TokenKind.R_BRACE)
                return Optional.of(new GrammerError(next.getRowNum(), next.getColNum(), "函数声明错误,缺少}"));
            try {
                table.previousLevel();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 防止未返回行为
            globalFunctions func = table.getGlobalFunc(funcName);
            if(func.getRtnType() == RtnType.VOID)
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.RET, 0, 0);
            else{
                // 如果没有返回的话，默认返回-19980711
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.ARGA, 0, 0);
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.PUSH, -19980711L, 0);
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.STORE64, 0, 0);
                ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.RET, 0, 0);
            }
        }
    }

    private void transInsert(String tokenName, TokenKind orgType, boolean isConstant, int locOffset) {
        if (isConstant)
            table.insertSymbol(tokenName, new SymbolEntry(tokenName, 0, SymbolType.CONSTANT, table.getCurrentLevel(), tokenKindSymbolTypeHashMap.get(orgType), locOffset));
        else
            table.insertSymbol(tokenName, new SymbolEntry(tokenName, 0, SymbolType.VARIABLE, table.getCurrentLevel(), tokenKindSymbolTypeHashMap.get(orgType), locOffset));
        SymbolEntry find = table.findSymbol(tokenName);
        int level = find.getLevel();
        if (level == 0){
            table.insertGlobalConst(tokenName, 'I', "0", isConstant);
            int offset = table.getGlobalConst(tokenName).getIndex();
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.GLOBA, (long)offset, 0);
        }
        else {
            ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.LOCA, table.findSymbol(tokenName).getOffset(), 0);
        }
        // 从符号表中获取层级和偏移，生成load指令获取地址
        Optional<Exception> re = AnalyseExpression();
        try {
            if (re.isPresent())
                throw re.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ins.pushBackInstruction(ins.getCodeOffset(), InstructionType.STORE64, 0, 0);
    }

    private Pair<Optional<Token>, Optional<Exception>> NextToken() {
        int size = tkList.size();
        if (tokenIndex < size)
            return new Pair<>(Optional.of(tkList.get(tokenIndex++)), Optional.empty());
        return new Pair<>(Optional.empty(), Optional.of(new TokenError(tkList.get(size - 1).getRowNum() + 1, 0, "已经是最后一个单词了")));
    }

    private Pair<Optional<Token>, Optional<Exception>> UnreadToken() {
        if (tokenIndex > 0)
            return new Pair<>(Optional.of(tkList.get(--tokenIndex)), Optional.empty());
        return new Pair<>(Optional.empty(), Optional.of(new TokenError(0, 0, "已经是最开始了，无法回退Token")));
    }
}
