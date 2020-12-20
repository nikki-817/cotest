package com.company.Instruction;

import com.company.SymbolTable.RtnType;
import com.company.SymbolTable.SymbolIndexTable;
import com.company.SymbolTable.globalConstants;
import com.company.SymbolTable.globalFunctions;
import jdk.nashorn.internal.runtime.GlobalFunctions;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

class ins {
    int index;
    InstructionType type;
    long op1;
    int op2;

    ins(int index, InstructionType type, long op1, int op2) {
        this.type = type;
        this.op1 = op1;
        this.op2 = op2;
        this.index = index;
    }
}

public class Instruction {
    private ArrayList<ins> instructionList = new ArrayList<>();
    private int codeOffset = 0;
    private static HashMap<InstructionType, Byte> insMap = new HashMap<>();
    {
        insMap.put(InstructionType.NOP, (byte)0x00);
        insMap.put(InstructionType.PUSH, (byte)0x01);
        insMap.put(InstructionType.POP, (byte)0x02);
        insMap.put(InstructionType.POPN, (byte)0x03);
        insMap.put(InstructionType.DUP, (byte)0x04);
        insMap.put(InstructionType.LOCA, (byte)0x0a);
        insMap.put(InstructionType.ARGA, (byte)0x0b);
        insMap.put(InstructionType.GLOBA, (byte)0x0c);
        insMap.put(InstructionType.LOAD8, (byte)0x10);
        insMap.put(InstructionType.LOAD16, (byte)0x11);
        insMap.put(InstructionType.LOAD32, (byte)0x12);
        insMap.put(InstructionType.LOAD64, (byte)0x13);
        insMap.put(InstructionType.STORE8, (byte)0x14);
        insMap.put(InstructionType.STORE16, (byte)0x15);
        insMap.put(InstructionType.STORE32, (byte)0x16);
        insMap.put(InstructionType.STORE64, (byte)0x17);
        insMap.put(InstructionType.ALLOC, (byte)0x18);
        insMap.put(InstructionType.FREE, (byte)0x19);
        insMap.put(InstructionType.STACKALLOC, (byte)0x1a);
        insMap.put(InstructionType.ADDI, (byte)0x20);
        insMap.put(InstructionType.SUBI, (byte)0x21);
        insMap.put(InstructionType.MULI, (byte)0x22);
        insMap.put(InstructionType.DIVI, (byte)0x23);
        //这里有4个浮点数相关的指令
        insMap.put(InstructionType.DIVU, (byte)0x28);
        insMap.put(InstructionType.SHL, (byte)0x29);
        insMap.put(InstructionType.SHR, (byte)0x2a);
        insMap.put(InstructionType.AND, (byte)0x2b);
        insMap.put(InstructionType.OR, (byte)0x2c);
        insMap.put(InstructionType.XOR, (byte)0x2d);
        insMap.put(InstructionType.NOT, (byte)0x2e);
        insMap.put(InstructionType.CMPI, (byte)0x30);
        insMap.put(InstructionType.CMPU, (byte)0x31);
        insMap.put(InstructionType.NEGI, (byte)0x34);
        insMap.put(InstructionType.ITOF, (byte)0x36);
        insMap.put(InstructionType.FTOI, (byte)0x37);
        insMap.put(InstructionType.SHRI, (byte)0x38);
        insMap.put(InstructionType.SETLT, (byte)0x39);
        insMap.put(InstructionType.SETGT, (byte)0x3a);
        insMap.put(InstructionType.BR, (byte)0x41);
        insMap.put(InstructionType.BRFALSE, (byte)0x42);
        insMap.put(InstructionType.BRTRUE, (byte)0x43);
        insMap.put(InstructionType.CALL, (byte)0x48);
        insMap.put(InstructionType.RET, (byte)0x49);
        insMap.put(InstructionType.CALLNAME, (byte)0x4a);
        insMap.put(InstructionType.SCANI, (byte)0x50);
        insMap.put(InstructionType.SCANC, (byte)0x51);
        insMap.put(InstructionType.PRINTI, (byte)0x54);
        insMap.put(InstructionType.PRINTC, (byte)0x55);
        insMap.put(InstructionType.PRINTS, (byte)0x57);
        insMap.put(InstructionType.PRINTLN, (byte)0x58);
        insMap.put(InstructionType.PANIC, (byte)0xfe);
    }

    public int getNextCodeOffset() {
        return codeOffset + 1;
    }

    public int getCodeOffset() {
        return codeOffset;
    }

    public void clearOffset() {
        this.codeOffset = 0;
    }

    public int pushBackInstruction(int insIndex, InstructionType type, long op1, int op2) {
        instructionList.add(new ins(insIndex, type, op1, op2));
        this.codeOffset++;
        return instructionList.size() - 1;
    }

    public void updateInstruction(int org, int op1, int op2) {
        instructionList.get(org).op1 = op1;
        instructionList.get(org).op2 = op2;
    }

    public void generateCode(SymbolIndexTable table, String out) throws IOException {
        ArrayList<Integer> arr = new ArrayList<>();
        int count = 0;
        for (ins instruction : instructionList) {
            if (instruction.index == 0) {
                arr.add(count);
                count = 0;
            }
            count++;
        }
        arr.add(count);
        File file = new File(out);
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            //magic
            os.write((byte)0x72);
            os.write((byte)0x30);
            os.write((byte)0x3b);
            os.write((byte)0x3e);
            //version
            os.write((byte)0x00);
            os.write((byte)0x00);
            os.write((byte)0x00);
            os.write((byte)0x01);
            table.InsertStart();
            writeConstants(os, table.getGblConsTable());
            writeNext(os, table.getGblFuncTable(), arr);
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeConstants(OutputStream os, ArrayList<globalConstants> constants) throws Exception {
        os.write(getLow(4, constants.size()));
        for (globalConstants constant : constants) {
            if (constant.isConstant())
                os.write((byte)0x01);
            else
                os.write((byte)0x00);
            if (constant.getType() == 'I'){
                os.write(getLow(4, 8L));
                os.write(getLow(8, 0L));
            }
            else {
                String val = constant.getVal();
                os.write(getLow(4, (long)val.length()));
                os.write(val.getBytes());
            }
        }
    }

    private void writeNext(OutputStream os, ArrayList<globalFunctions> funcs, ArrayList<Integer> arr) throws Exception {
        os.write(getLow(4, (long)funcs.size()));
        boolean sm = false;
        int funcIndex = 0;
        int j = 0;
        int cnt = 0;
        for (int i = 1; i < arr.size(); i++) {
            globalFunctions fun = funcs.get(i - 1);
            os.write(getLow(4, fun.getName_index()));
            if (fun.getRtnType() == RtnType.VOID){
                os.write(getLow(4,0L));
            }
            else {
                os.write(getLow(4, 1L));
            }
            os.write(getLow(4, (long)fun.getParams_size()));
            os.write(getLow(4, (long)fun.getCount()));
            if (i == 1)
                os.write(getLow(4, (long)(arr.get(i) + 3)));
            else
                os.write(getLow(4, (long)(arr.get(i))));
            for (int k = 0; k < arr.get(i); k++) {
                ins instruction = instructionList.get(cnt + k);
                System.out.print(instruction.type);
                byte op[];
                os.write(insMap.get(instruction.type));
                getObjByType(instruction.type, os, instruction);
            }
            if (i==1){
                for(globalFunctions item : funcs){
                    if (item.getName().equals("main")){
                        os.write((byte)0x1a);
                        int size = 0;
                        if (item.getRtnType() != RtnType.VOID)
                            ++size;
                        size += item.getParams_size();
                        os.write(getLow(4, size));
                        os.write((byte)0x48);
                        os.write(getLow(4, item.getIndex()));
                        size -= item.getParams_size();
                        os.write((byte)0x03);
                        os.write(getLow(4, size));
                    }
                }
                System.out.println("这里是call");
            }
            cnt += arr.get(i);
        }

    }

    private void getObjByType(InstructionType type, OutputStream os, ins instruction) throws IOException {
        byte[] op;
        switch (needParaNum(type)) {
            case 1:
                if (instruction.type == InstructionType.PUSH) {
                    op = getLow(8, instruction.op1);
                    os.write(op);
                } else if (instruction.type == InstructionType.POPN) {
                    op = getLow(4, instruction.op1);
                    os.write(op);
                } else if (instruction.type == InstructionType.LOCA) {
                    op = getLow(4, instruction.op1);
                    os.write(op);
                } else if (instruction.type == InstructionType.ARGA) {
                    op = getLow(4, instruction.op1);
                    os.write(op);
                } else if (instruction.type == InstructionType.GLOBA) {
                    op = getLow(4, instruction.op1);
                    os.write(op);
                } else if (instruction.type == InstructionType.STACKALLOC) {
                    op = getLow(4, instruction.op1);
                    os.write(op);
                } else if (instruction.type == InstructionType.BR) {
                    op = getLow(4, instruction.op1);
                    os.write(op);
                } else if (instruction.type == InstructionType.BRFALSE) {
                    op = getLow(4, instruction.op1);
                    os.write(op);
                } else if (instruction.type == InstructionType.BRTRUE) {
                    op = getLow(4, instruction.op1);
                    os.write(op);
                } else if (instruction.type == InstructionType.CALL) {
                    op = getLow(4, instruction.op1);
                    os.write(op);
                } else if (instruction.type == InstructionType.CALLNAME) {
                    op = getLow(4, instruction.op1);
                    os.write(op);
                }
                System.out.println(" " + instruction.op1);
                break;
            case 0:
                System.out.println("");
                break;
        }
    }

    private int needParaNum(InstructionType type) {
        if (type == InstructionType.PUSH || type == InstructionType.POPN || type == InstructionType.LOCA || type == InstructionType.ARGA || type == InstructionType.GLOBA || type == InstructionType.STACKALLOC || type == InstructionType.BR || type == InstructionType.BRFALSE || type == InstructionType.BRTRUE || type == InstructionType.CALL || type == InstructionType.CALLNAME)
            return 1;
        else
            return 0;
    }

    private byte[] getLow(int n, long in) {
        byte[] bt = new byte[n];
        for (int i = 1; i <= n; i++) {
            int shiftNum = 8 * (n - i);
            bt[i - 1] = (byte) ((in >> shiftNum) & 0xff);
        }
        return bt;
    }


}
