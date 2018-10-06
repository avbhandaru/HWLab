package edu.cornell.cs3410;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProgramAssembler {

    private static final int NO_OP = 0;

    private ProgramAssembler() {
        // Private; cannot construct.
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            System.err.println("usage: ProgramAssembler <mips-asm-file>");
            System.exit(1);
        }
        Listing code = new Listing();
        try {
            code.load(new File(args[0]));
            for (Segment segment : code.seg) {
                for (int i = 0; i < segment.data.length; i++) {
                    int pc = segment.start_pc + i;
                    int instr = code.instr(pc);
                    System.out.println(toHex(pc*4, 8)+" : " + toHex(instr, 8) + " : " + disassemble(instr, pc*4));
                }
                System.out.println();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Segment implements Cloneable {
        public int start_pc;
        public int data[];
        public Segment(int pc, int d[]) { start_pc = pc; data = d; }
    }

    static class Listing implements Cloneable {
        private String src = "";
        private Segment seg[] = new Segment[0];
        private ProgramState state;
        private ArrayList<String> src_lines = new ArrayList<String>();
        private ArrayList<Integer> addr_map = new ArrayList<Integer>();

        public Listing() {}
        public Listing(String value) throws IOException {
            setSource(value);
        }

        public void setListener(ProgramState state) {
            this.state = state;
        }
        public void load(File file) throws IOException {
            String s = readFully(file);
            setSource(s);
        }

        public ProgramState getState() { return state; }

        public String getSource() {
            return src;
        }

        public int getLineCount() {
            return src_lines.size();
        }

        public String getLine(int index) {
            return src_lines.get(index);
        }

        public int getAddressOf(int index) {
            Integer i = addr_map.get(index);
            if (i == null) {
                return -1;
            }
            return i.intValue();
        }

        public void setSource(String s) throws IOException {
            ArrayList<String> sl = splitLines(s);
            ArrayList<Integer> am = new ArrayList<Integer>();
            seg = assemble(sl, 0, am);
            src = s;
            addr_map = am;
            src_lines = sl;
        }

        public boolean isEmpty() {
            return seg.length == 0;
        }

        int instr(int i) {
            Segment s = segmentOf(i);
            if (s != null)
                return s.data[i - s.start_pc];
            else
                return NO_OP;
        }

        Segment segmentOf(int i) {
            for (int s = 0; s < seg.length; s++) {
                if (i >= seg[s].start_pc && i < seg[s].start_pc + seg[s].data.length) {
                //System.out.println("segment of "+i+" is "+seg[s].start_pc);
                return seg[s];
                }
            }
            //System.out.println("segment of "+i+" is null");
            return null;
        }


        @Override
        public Listing clone() {
            try {
                return (Listing) super.clone();
            }
            catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }
    
    private static String toHex(int i, int digits) {
        if (digits > 8) digits = 8;
        String s = Long.toHexString(i & 0xffffffffL);
        if (s.length() >= digits) return "0x"+s;
        else return "0x00000000".substring(0, 2+digits-s.length()) + s;
    }
    
    private static String readFully(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        StringBuffer buf = new StringBuffer();
        while ((line = in.readLine()) != null)
            buf.append(line+"\n");
        return buf.toString();
    }

    private static ArrayList<String> splitLines(String src) throws IOException {
        BufferedReader in = new BufferedReader(new StringReader(src));
        String line;
        ArrayList<String> buf = new ArrayList<String>();
        while ((line = in.readLine()) != null)
            buf.add(line);
        return buf;
    }

    private static Pattern pat0 = Pattern.compile("\\s+");
    private static Pattern pat1 = Pattern.compile("\\s*,\\s*");

    private static ArrayList<String> normalize(ArrayList<String> lines) {
        ArrayList<String> res = new ArrayList<String>();
        for (int lineno = 0; lineno < lines.size(); lineno++) {
            String line = lines.get(lineno);
            int i = line.indexOf('#');
            if (i == 0) line = "";
            else if (i > 0) line = line.substring(0, i);
            line = line.trim();
            line = pat0.matcher(line).replaceAll(" ");
            line = pat1.matcher(line).replaceAll(",");
            if (line.length() == 0) res.add(null);
            else res.add(line);
        }
        return res;
    }

    // some patterns
    static String _reg = "\\$(\\d+|zero|at|v[01]|a[0-3]|t[0-9]|s[0-7]|k[01]|gp|sp|fp|ra)";
    static String __hex = "0x[a-fA-F0-9]+";
    static String __decimal = "-?\\d+";
    static String __label = "[a-zA-Z]\\w*";
    static String _imm = "("+__hex+"|"+__decimal+"|"+__label+")";

    private static int parseSegmentAddress(int lineno, String addr) throws IOException {
        if (addr.toLowerCase().startsWith("0x"))
            return Integer.parseInt(addr.substring(2), 16);
        char c = addr.charAt(0);
        if ((c >= '0' && c <= '9'))
            return Integer.parseInt(addr);
        throw new ParseException("Line " + (lineno+1) + ": illegal address '"+addr+"' in assembly directive");
    }

    static class ParseException extends IOException {
        private static final long serialVersionUID = 4648870901015801834L;
        StringBuffer msg;
        int count = 0;
        public ParseException() { msg = new StringBuffer(); }
        public ParseException(String m) { this(); add(m); }
        public void add(String m) { msg.append("\n"); msg.append(m); count++; }
        public void add(ParseException e) { msg.append(e.msg.toString()); count += e.getCount(); }
        public String getMessage() { return "Assembling MIPS instructions: " +count+(count == 1 ? " error:":" errors:") + msg.toString(); }
        public int getCount() { return count; }
    }
    

    static Pattern pat_label = Pattern.compile("("+__label+")");
    private static HashMap<String, Integer> pass1(ArrayList<String> lines, int start_address, ArrayList<Integer> addr_map) throws IOException {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        int addr = start_address;
        addr_map.clear();
        ParseException err = new ParseException();
        for (int lineno = 0; lineno < lines.size(); lineno++) {
            String line = lines.get(lineno);
            if (line == null) {
                addr_map.add(null);
                continue;
            }
            int i;
            if (line.toLowerCase().startsWith(".text")) {
                i = line.indexOf(' ');
                if (i > 0) {
                    try {
                        int a = parseSegmentAddress(lineno, line.substring(i+1));
                        if ((a & 3) != 0)
                        err.add("Line " + (lineno+1) + ": mis-aligned address '"+line.substring(i+1)+"' in .text assembly directive");
                        addr = a & ~3;
                    }
                    catch (ParseException e){
                        err.add(e);
                    }
                }
                addr_map.add(null);
                continue;
            }
            if (line.toLowerCase().startsWith(".word")) {
                addr_map.add(new Integer(addr));
                addr += 4;
                continue;
            }
            if (line.startsWith(".")) {
                err.add("Line " + (lineno+1) + ": unrecognized assembly directive '"+line+"'");
                continue;
            }

            i = line.indexOf(':');
            if (i >= 0) {
                String name = line.substring(0, i).trim();
                if (name.length() == 0) {
                    err.add("Line " + (lineno+1) + ": expected label name before ':'");
                    continue;
                }
                Matcher m = pat_label.matcher(name);
                if (name.equalsIgnoreCase("pc") || !m.matches()) {
                    err.add("Line " + (lineno+1) + ": illegal label name '"+name+"' before ':'");
                    continue;
                }
                map.put(name, new Integer(addr));
                if (i < line.length()-1) {
                    // label: instruction
                    line = line.substring(i+1).trim();
                    lines.set(lineno, line);
                    addr_map.add(new Integer(addr));
                    addr += 4;
                } else {
                    // label:
                    addr_map.add(null);
                    lines.set(lineno, null);
                }
            } else {
                addr_map.add(new Integer(addr));
                addr += 4;
            }
        }
        if (err.getCount() > 0)
            throw err;
        return map;
    }

    static HashMap<String, Command> cmds = new HashMap<String, Command>();
    static HashMap<Integer, Command> opcodes = new HashMap<Integer, Command>();
    static HashMap<Integer, Command> fcodes = new HashMap<Integer, Command>();
    static HashMap<Integer, Command> socodes = new HashMap<Integer, Command>();

    // returns an n-bit number (with leading zeros for n<32).
    // if SIGNED_ABSOLUTE, the accepted inputs are:
    //   - hex (with no more than n bits)
    //   - decimal (positive or negative values in the range -2^(n-1)..2^(n-1)-1)
    //   - "pc" (as long as it is in the range)
    //   - label (as long as it is in the range)
    // if UNSIGNED_ABSOLUTE, the accepted inputs are:
    //   - as above, but no negative decimals, and with a range check of 0..2^n-1 instead
    // if SIGNED_RELATIVE, the accepted inputs are:
    //   - hex or decimal, as above (no relative offsetting)
    //   - "pc" or label, minus (addr+4) (as long as this result is in the range)
    // if ANY_ABSOLUTE, the accepted inputs are:
    //   - anything that fits in n bits)
    private static enum Type {
        SIGNED_RELATIVE,
        SIGNED_ABSOLUTE,
        UNSIGNED_ABSOLUTE,
        ANY_ABSOLUTE
    };
    private static int resolve(int lineno, String imm, int addr, HashMap<String, Integer> sym, Type type, int nbits) throws IOException {
        int offset = (type == Type.SIGNED_RELATIVE ? addr+4 : 0);
        long min = (type == Type.UNSIGNED_ABSOLUTE ? 0 : (-1L << (nbits-1)));
        long max = (type == Type.UNSIGNED_ABSOLUTE ? ((1L << nbits)-1) : ((1L << (nbits-1)) - 1));
        int mask = (int)(1L << nbits) - 1;
        long val;
        try {
            if (imm.length() == 0)
                throw new NumberFormatException();
            char c = imm.charAt(0);
            if (imm.equalsIgnoreCase("pc")) {
                val = ((long)addr & 0xffffffffL) - offset;
            }
            else if (imm.toLowerCase().startsWith("0x")) {
                val = Long.parseLong(imm.substring(2), 16);
                // nb: this check is different,
                // to allow 0xffff to mean "-1" for signed abs/relative,
                // but 65535 for unsigned absolute
                if ((val & mask) != val) {
                    throw new ParseException("Line "+(lineno+1)+": overflow in "+type+" '"+imm+"' ("+nbits+" bits maximum)");
                }
                return (int)(val & mask);
            }
            else if ((c == '-') || (c >= '0' && c <= '9')) {
                val = Long.parseLong(imm);
            }
            else {
                Integer a = sym.get(imm);
                if (a == null)
                throw new ParseException("Line "+(lineno+1)+": expecting "+type+", but no such label or number '"+imm+"'");
                val = ((long)a.intValue() & 0xffffffffL) - offset;
                imm = imm + " ("+val+")";
            }
            if (type == Type.ANY_ABSOLUTE) {
                // nb: this check is different,
                // to allow 0xffff to mean "-1" for signed abs/relative,
                // but 65535 for unsigned absolute
                if ((val & mask) != val) {
                    throw new ParseException("Line "+(lineno+1)+": overflow in "+type+" '"+imm+"' ("+nbits+" bits maximum)");
                }
            }
            else {
                if (val < min || val > max) {
                    throw new ParseException("Line "+(lineno+1)+": overflow in "+type+" '"+imm+"' : allowed range is "+min+" ("+toHex((int)min & mask, 1)+") to "+max+" ("+toHex((int)max & mask, 1)+")");
                }
            }
            return (int)(val & mask);
        }
        catch (NumberFormatException e) {
            throw new ParseException("Line "+(lineno+1)+": invalid "+type+" '"+imm+"'");
        }
    }

    private static abstract class Command {
        String name;
        int opcode;
        Command(String name, int op) {
            this.name = name;
            opcode = op;
            cmds.put(name, this);
        }
        abstract String decode(int addr, int instr);
        abstract int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException;
    }

    private static class Nop extends Command {
        Nop(String name, int op) { super(name, op); }
        String decode(int addr, int instr) {
            return name;
        }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> hashmap) throws IOException {
            return 0;
        }
    }

    private static class Syscall extends Command {
      Syscall(String name, int op) { 
        super(name, op);
        opcodes.put(new Integer(op), this);
      }
      String decode(int addr, int instr) {
        return name;
      }
      int encode (int lineno, int addr, String args, HashMap<String, Integer> hashmap) throws IOException {
        return 6;
      }
    }

    private static class Eret extends Command {
      Eret(String name, int op) { 
        super(name, op); 
        opcodes.put(new Integer(op), this);
      }
      String decode(int addr, int instr) {
        return name;
      }
      int encode (int lineno, int addr, String args, HashMap<String, Integer> hashmap) throws IOException {
        return (8 << 26) | (1 << 25) | 6;
      }
    }

    static Pattern pat_word = Pattern.compile(_imm);
    private static class Word extends Command {
        Word(String name, int op) {
            super(name, op);
            opcodes.put(new Integer(op), this);
        }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_word.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects integer argument");
            }
            int word = resolve(lineno, m.group(1), addr, sym, Type.ANY_ABSOLUTE, 32);
            return word;
        }
        String decode(int addr, int instr) {
            return name+" "+toHex(instr, 8);
        }
    }

    private static int reg(String r) throws NumberFormatException {
        switch (r.charAt(0)) {
        case 'z': // zero
            return 0;
        case 'a':
            if (r.equals("at")) return 1;
            else return 4 + Integer.parseInt(r.substring(1));
        case 'v':
            return 2 + Integer.parseInt(r.substring(1));
        case 't':
            int i = Integer.parseInt(r.substring(1));
            if (i <= 7) return 8 + i;
            else return 24 + (i-8);
        case 's':
            if (r.equals("sp")) return 29;
            else return 16 + Integer.parseInt(r.substring(1));
        case 'k':
            return 26 + Integer.parseInt(r.substring(1));
        case 'g': // gp
            return 28;
        case 'f': // fp
            return 30;
        case 'r': // ra
            return 31;
        default:
            return Integer.parseInt(r);
        }
    }

    private static abstract class IType extends Command {
        IType(String name, int op) {
            super(name, op);
            opcodes.put(new Integer(op), this);
        }
        int encode(String rd, String rs, int imm, int lineno) throws IOException {
            try {
                int dest = reg(rd);
                int src = reg(rs);
                imm = imm & 0x0000ffff;
                if ((dest & 0x1f) != dest) {
                    throw new ParseException("Line "+(lineno+1)+": invalid destination register: $"+dest);
                }
                if ((src & 0x1f) != src) {
                    throw new ParseException("Line "+(lineno+1)+": invalid source register: $"+src);
                }
                return (opcode << 26) | (src << 21) | (dest << 16) | imm;
            }
            catch (NumberFormatException e) {
                throw new ParseException("Line "+(lineno+1)+": invalid arguments to '"+name+"': "+e.getMessage());
            }
        }
        String rD(int instr) { return "$"+((instr >> 16)&0x1f); }
        String rS(int instr) { return "$"+((instr >> 21)&0x1f); }
        String sImm(int instr) { return toHex(instr & 0x0000ffff, 4); }
    }
    
    static Pattern pat_arith_imm = Pattern.compile(_reg+","+_reg+","+_imm);
    private static class ArithImm extends IType {
        Type itype;
        ArithImm(String name, int op, boolean signed) {
            super(name, op);
            itype = (signed ? Type.SIGNED_ABSOLUTE : Type.UNSIGNED_ABSOLUTE);
        }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_arith_imm.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, $S, "+itype);
            }
            int imm = resolve(lineno, m.group(3), addr, sym, itype, 16);
            return encode(m.group(1), m.group(2), imm, lineno);
        }
        String decode(int addr, int instr) {
            return name+" "+rD(instr)+", "+rS(instr)+", "+sImm(instr);
        }
    }

    static Pattern pat_lui = Pattern.compile(_reg+","+_imm);
    private static class Lui extends IType {
        Lui(String name, int op) { super(name, op); }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_lui.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, imm");
            }
            int imm = resolve(lineno, m.group(2), addr, sym, Type.ANY_ABSOLUTE, 16);
            return encode(m.group(1), "0", imm, lineno);
        }
        String decode(int addr, int instr) {
            return name+" "+rD(instr)+", "+sImm(instr);
        }
    }

    static Pattern pat_mem = Pattern.compile(_reg+","+_imm+"\\("+_reg+"\\)");
    private static class Mem extends IType {
        Mem(String name, int op) { super(name, op); }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_mem.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, signed_imm($S)");
            }
            int imm = resolve(lineno, m.group(2), addr, sym, Type.SIGNED_ABSOLUTE, 16);
            return encode(m.group(1), m.group(3), imm, lineno);
        }
        String decode(int addr, int instr) {
            return name+" "+rD(instr)+", "+sImm(instr)+"("+rS(instr)+")";
        }
    }

    static Pattern pat_br = Pattern.compile(_reg+","+_reg+","+_imm);
    private static class Br extends IType {
        Br(String name, int op) { super(name, op); }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_br.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $S, $T, offset or label");
            }
            int offset = resolve(lineno, m.group(3), addr, sym, Type.SIGNED_RELATIVE, 18);
            if ((offset & 0x3) != 0) {
                throw new ParseException("Line "+(lineno+1)+": mis-aligned offset in '"+name+"'");
            }
            return encode(m.group(2), m.group(1), offset >> 2, lineno);
        }
        String decode(int addr, int instr) {
            return name+" "+rS(instr)+", "+rD(instr)+", "+sImm(instr<<2);
        }
    }

    static Pattern pat_bz = Pattern.compile(_reg+","+_imm);
    private static class Bz extends IType {
        Bz(String name, int op) { super(name, op); }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_bz.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $S, offset or label");
            }
            int offset = resolve(lineno, m.group(2), addr, sym, Type.SIGNED_RELATIVE, 18);
            if ((offset & 0x3) != 0) {
                throw new ParseException("Line "+(lineno+1)+": mis-aligned offset in '"+name+"'");
            }
            return encode("0", m.group(1), offset >> 2, lineno);
        }
        String decode(int addr, int instr) {
            return name+" "+rS(instr)+", "+sImm(instr<<2);
        }
    }

    static Pattern pat_j0 = Pattern.compile(_imm);
    private static class J extends Command {
        J(String name, int op) {
            super(name, op);
            opcodes.put(new Integer(op), this);
        }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_j0.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects address or label");
            }
            int absaddr = resolve(lineno, m.group(1), addr, sym, Type.UNSIGNED_ABSOLUTE, 32);
            if ((absaddr & 0x3) != 0) {
                throw new ParseException("Line "+(lineno+1)+": mis-aligned address in '"+name+"'");
            }
            if ((absaddr & 0xf0000000) != ((addr+4) & 0xf0000000)) {
                throw new ParseException("Line "+(lineno+1)+": overflow in address in '"+name+"': can't jump from "+toHex(addr, 8)+" to " + toHex(absaddr, 8));
            }
            return (opcode << 26) | ((absaddr>>2) & 0x03ffffff);
        }
        String decode(int addr, int instr) {
            return name+" "+toHex(((addr+4)&0xf0000000)|((instr & 0x03ffffff)<<2), 8);
        }
    }

    private static abstract class RType extends Command {
        int f;
        RType(String name, int zero, int f) {
            super(name, 0);
            this.f = f;
            fcodes.put(new Integer(f), this);
        }
        int encode(String rd, String rs, String rt, int sa, int lineno) throws IOException {
            try {
                int dest = reg(rd);
                int src = reg(rs);
                int trg = reg(rt);
                if ((dest & 0x1f) != dest) {
                    throw new ParseException("Line "+(lineno+1)+": invalid destination register: $"+dest);
                }
                if ((src & 0x1f) != src) {
                    throw new ParseException("Line "+(lineno+1)+": invalid source1 register: $"+src);
                }
                if ((trg & 0x1f) != trg) {
                    throw new ParseException("Line "+(lineno+1)+": invalid source2 register: $"+trg);
                }
                if ((sa & 0x1f) != sa) {
                    throw new ParseException("Line "+(lineno+1)+": invalid shift amount: "+sa);
                }
                return (opcode << 26) | (src << 21) | (trg << 16) | (dest << 11) | (sa << 6) | f;
            }
            catch (NumberFormatException e) {
                throw new ParseException("Line "+(lineno+1)+": invalid arguments to '"+name+"': "+e.getMessage());
            }
        }
        String rD(int instr) { return "$"+((instr >> 11)&0x1f); }
        String rS(int instr) { return "$"+((instr >> 21)&0x1f); }
        String rT(int instr) { return "$"+((instr >> 16)&0x1f); }
        String sSa(int instr) { return ""+((instr >> 6)&0x1f); }
    }

    private static abstract class CopType extends Command {
        int f;
        CopType(String name, int zero, int f) {
            super(name, 0);
            this.f = f;
            fcodes.put(new Integer(f), this);
        }
        int encode(String rd, String rt, int func, int lineno) throws IOException {
            try {
                int dest = reg(rd);
                int src = reg(rt);
                if ((dest & 0x1f) != dest) {
                    throw new ParseException("Line "+(lineno+1)+": invalid destination register: $"+dest);
                }
                if ((src & 0x1f) != src) {
                    throw new ParseException("Line "+(lineno+1)+": invalid source1 register: $"+src);
                }
                if ((func & 0x1f) != func) {
                    throw new ParseException("Line "+(lineno+1)+": invalid shift amount: "+func);
                }
                return (opcode << 26) | (func << 21) | (src << 16) | (dest << 11) | f;
            }
            catch (NumberFormatException e) {
                throw new ParseException("Line "+(lineno+1)+": invalid arguments to '"+name+"': "+e.getMessage());
            }
        }
        String rD(int instr) { return "$"+((instr >> 11)&0x1f); }
        String rT(int instr) { return "$"+((instr >> 16)&0x1f); }
        String sSa(int instr) { return ""+((instr >> 6)&0x1f); }
    }



    static Pattern pat_arith_reg = Pattern.compile(_reg+","+_reg+","+_reg);
    private static class ArithReg extends RType {
        ArithReg(String name, int zero, int f) { super(name, zero, f); }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_arith_reg.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, $S, $T");
            }
            return encode(m.group(1), m.group(2), m.group(3), 0, lineno);
        }
        String decode(int addr, int instr) {
            return name+" "+rD(instr)+", "+rS(instr)+", "+rT(instr);
        }
    }

    static Pattern pat_shift_c = Pattern.compile(_reg+","+_reg+","+_imm);
    private static class ShiftConstant extends RType {
        ShiftConstant(String name, int zero, int f) { super(name, zero, f); }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_shift_c.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, $T, sa");
            }
            int sa = resolve(lineno, m.group(3), addr, sym, Type.UNSIGNED_ABSOLUTE, 5);
            return encode(m.group(1), "0", m.group(2), sa, lineno);
        }
        String decode(int addr, int instr) {
            return name+" "+rD(instr)+", "+rT(instr)+", "+sSa(instr);
        }
    }

    static Pattern pat_shift_v = Pattern.compile(_reg+","+_reg+","+_reg);
    private static class ShiftVariable extends RType {
        ShiftVariable(String name, int zero, int f) { super(name, zero, f); }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_shift_v.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $D, $T, $S");
            }
            return encode(m.group(1), m.group(3), m.group(2), 0, lineno);
        }
        String decode(int addr, int instr) {
            return name+" "+rD(instr)+", "+rT(instr)+", "+rS(instr);
        }
    }

    static Pattern pat_coproc_mov = Pattern.compile(_reg+","+_reg);
    private static class CoprocMove extends CopType {
        CoprocMove(String name, int zero, int f) { super(name, zero, f); }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_coproc_mov.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $T, $D");
            }
            return encode(m.group(2), m.group(1), 0, lineno);
        }
        String decode(int addr, int instr) {
            return name+" "+rT(instr)+", "+rD(instr);
        }
    }

    static Pattern pat_jr = Pattern.compile(_reg);
    private static class Jr extends RType {
        Jr(String name, int zero, int f) { super(name, zero, f); }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_jr.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $S");
            }
            return encode("0", m.group(1), "0", 0, lineno);
        }
        String decode(int addr, int instr) {
            return name+" "+rS(instr);
        }
    }

    static Pattern pat_jalr = Pattern.compile(_reg + "(?:," + _reg + ")?");
    private static class Jalr extends RType {
        Jalr(String name, int zero, int f) { super(name, zero, f); }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_jalr.matcher(args);
            if(!m.matches()) {
                throw new ParseException("Line " + (lineno + 1) + ": '" + name + "' expects $S");
            }
            else {
                boolean haveOpt = m.group(2) != null;
                String rd = haveOpt ? m.group(1) : "31";
                String rs = m.group(haveOpt ? 2 : 1);
                return encode(rd, rs, "0", 0, lineno);
            }
        }

        String decode(int addr, int instr) {
            if("31".equals(rD(instr))) {
                return name + " " + rS(instr);
            }
            else {
                return name + " " + rD(instr) + ", " + rS(instr);
            }
        }
    }

    static Pattern pat_regimm = Pattern.compile(_reg+","+_imm);
    private static class RegImm extends Command {
        int subop;
        RegImm(String name, int subop) {
            super(name, 1);
            this.subop = subop;
            socodes.put(new Integer(subop), this);
        }
        int encode(int lineno, int addr, String args, HashMap<String, Integer> sym) throws IOException {
            Matcher m = pat_regimm.matcher(args);
            if (!m.matches()) {
                throw new ParseException("Line "+(lineno+1)+": '"+name+"' expects $S, offset or label");
            }
            int offset = resolve(lineno, m.group(2), addr, sym, Type.SIGNED_RELATIVE, 18);
            if ((offset & 0x3) != 0) {
                throw new ParseException("Line "+(lineno+1)+": mis-aligned offset in '"+name+"'");
            }
            try {
                int src = reg(m.group(1));
                int imm = (offset >> 2) & 0x0000ffff;
                if ((subop & 0x1f) != subop) {
                    throw new ParseException("Line "+(lineno+1)+": invalid sub-operation: $"+subop);
                }
                if ((src & 0x1f) != src) {
                    throw new ParseException("Line "+(lineno+1)+": invalid source register: $"+src);
                }
                return (1 << 26) | (src << 21) | (subop << 16) | imm;
            }
            catch (NumberFormatException e) {
                throw new ParseException("Line "+(lineno+1)+": invalid arguments to '"+name+"': "+e.getMessage());
            }
        }
        String decode(int addr, int instr) {
            return name+" "+rS(instr)+", "+sImm(instr<<2);
        }
        String rS(int instr) { return "$"+((instr >> 21)&0x1f); }
        String sImm(int instr) { return toHex(instr & 0x0000ffff, 4); }
    }

    static {
        new Word(".word", -1);
        new Nop("nop", 0x0);
        new Syscall("syscall", 0x10);
        new Eret("eret", 0x8);
        new J("j", 0x2);
        new J("jal", 0x3);
        new Br("beq", 0x4);
        new Br("bne", 0x5);
        new Bz("blez", 0x6);
        new Bz("bgtz", 0x7);
        new RegImm("bltz", 0x0);
        new RegImm("bgez", 0x1);
        new ArithImm("addiu", 0x9, true);
        new ArithImm("slti", 0xa, true);
        new ArithImm("sltiu", 0xb, true);
        new ArithImm("andi", 0xc, false);
        new ArithImm("ori", 0xd, false);
        new ArithImm("xori", 0xe, false);
        new Lui("lui", 0xf);
        new Mem("lw", 0x23);
        new Mem("lb", 0x20);
        new Mem("lbu", 0x24);
        new Mem("sw", 0x2b);
        new Mem("sb", 0x28);
        new ArithReg("addu", 0, 0x21);
        new ArithReg("subu", 0, 0x23);
        new ArithReg("and", 0, 0x24);
        new ArithReg("or", 0, 0x25);
        new ArithReg("xor", 0, 0x26);
        new ArithReg("nor", 0, 0x27);
        new ArithReg("slt", 0, 0x2a);
        new ArithReg("sltu", 0, 0x2b);
        new ArithReg("movz", 0, 0x0a);
        new ArithReg("movn", 0, 0x0b);
        new ShiftConstant("sll", 0, 0x00);
        new ShiftConstant("srl", 0, 0x02);
        new ShiftConstant("sra", 0, 0x03);
        new ShiftVariable("sllv", 0, 0x04);
        new ShiftVariable("srlv", 0, 0x06);
        new ShiftVariable("srav", 0, 0x07);
        new CoprocMove("mfc0",0x10, 1);   // assemble mfc0 to binary is not done! -hanw
        new CoprocMove("mtc0",0x10, 0x05);
        new Jr("jr", 0, 0x08);
        new Jalr("jalr", 0, 0x09);
    }

    private static Segment[] pass2(ArrayList<String> lines, int start_address, HashMap<String, Integer> sym) throws IOException {
        ParseException err = new ParseException();
        int addr = start_address;
        int cnt = 0;
        ArrayList<Segment> seglist = new ArrayList<Segment>();
        int pc = start_address >>> 2;
        for (int lineno = 0; lineno < lines.size(); lineno++) {
            String line = lines.get(lineno);
            if (line == null) continue;
            if (line.toLowerCase().startsWith(".text ")) {
                if (cnt > 0) {
                    seglist.add(new Segment(pc, new int[cnt]));
                }
                cnt = 0;
                pc = parseSegmentAddress(lineno, line.substring(line.indexOf(' ')+1)) >>> 2;
            }
            else {
                cnt++;
            }
        }
        if (cnt > 0) {
            seglist.add(new Segment(pc, new int[cnt]));
        }
        Segment[] seg = new Segment[seglist.size()];
        if (seg.length == 0) {
            return seg;
        }
        for (int s = 0; s < seg.length; s++) {
            seg[s] = seglist.get(s);
            for (int s2 = 0; s2 < s; s2++) {
            if (seg[s].start_pc < seg[s2].start_pc + seg[s2].data.length &&
                seg[s2].start_pc < seg[s].start_pc + seg[s].data.length)
                err.add("Assembly segment at "+toHex(seg[s].start_pc*4, 8)+".."+toHex((seg[s].start_pc+seg[s].data.length)*4, 8)+" overlaps with segment at "+
                        toHex(seg[s2].start_pc*4, 8)+".."+toHex((seg[s2].start_pc+seg[s2].data.length)*4, 8));
            }
        }

        int cs = 0;
        cnt = 0;
        for (int lineno = 0; lineno < lines.size(); lineno++) {
            String line = lines.get(lineno);
            if (line == null) {
                continue;
            }
            int i = line.indexOf(' ');
            String instr = i >= 0 ? line.substring(0, i) : line;
            String args = i >= 0 ? line.substring(i+1) : "";
            if (instr.equalsIgnoreCase(".text")) {
                cs = -1;
                pc = parseSegmentAddress(lineno, line.substring(line.indexOf(' ')+1)) >>> 2;
                addr = pc << 2;
                cnt = 0;
                for (int s = 0; s < seg.length; s++) {
                    if (seg[s].start_pc == pc) {
                        cs = s;
                        break;
                    }
                }
                if (cs < 0) {
                    err.add("Line " + (lineno+1)+": internal error: bad segment");
                }
            }
            else {
                Command cmd = cmds.get(instr.toLowerCase());
                if (cmd == null) {
                    err.add("Line " + (lineno+1)+": unrecognized instruction: '"+instr+"'");
                }
                else if (cs >= 0) {
                    try {
                        seg[cs].data[cnt++] = cmd.encode(lineno, addr, args, sym);
                    }
                    catch (ParseException e) {
                        err.add(e);
                    }
                    addr += 4;
                }
            }
        }
        if (err.getCount() > 0) {
            throw err;
        }
        return seg;
    }

    private static Segment[] assemble(ArrayList<String> src_lines, int start_address, ArrayList<Integer> addr_map) throws IOException {
        ArrayList<String> lines = normalize(src_lines);
        HashMap<String, Integer> sym = pass1(lines, start_address, addr_map);
        return pass2(lines, start_address, sym);
    }

    private static String disassemble(int code[], int start_addr) throws IOException {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < code.length; i++) {
            int instr = code[i];
            int op = (instr >> 26) & 0x3f;
            Command cmd;
            if (op == 0) {
                int f = (instr & 0x3f);
                cmd = fcodes.get(new Integer(f));
            }
            else if (op == 1) {
                int subop = ((instr >> 16) & 0x1f);
                cmd = socodes.get(new Integer(subop));
            }
            else if (op == 0x10){
              int cop = ((instr >> 21) & 0x3f);
              cmd = fcodes.get(new Integer(cop));
            }
            else {
                cmd = opcodes.get(new Integer(op));
            }
            if (cmd == null) {
                throw new ParseException("Instruction " + (i+1)+" unrecognized: "+toHex(instr, 8));
            }
            buf.append(cmd.decode(start_addr+4*i, instr)+"\n");
        }
        return buf.toString();
    }

    static String disassemble(int instr, int addr) {
        int op = (instr >> 26) & 0x3f;
        Command cmd;
        if (op == 0) {
            int f = (instr & 0x3f);
            cmd = fcodes.get(new Integer(f));
        }
        else if (op == 1) {
            int subop = ((instr >> 16) & 0x1f);
            cmd = socodes.get(new Integer(subop));
        }
        else if (op == 0x10){
          int f = ((instr >> 21) & 0x3f);
          cmd = fcodes.get(new Integer(f));
        }
        else {
            cmd = opcodes.get(new Integer(op));
        }
        if (cmd == null) {
            cmd = opcodes.get(new Integer(-1));
        }
        return cmd.decode(addr, instr);
    }

}
