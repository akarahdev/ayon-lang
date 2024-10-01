package dev.akarah.ir;

import dev.akarah.ir.inst.Instruction;

import java.util.UUID;

public class SSABuilder {
    public static char[] letters = new char[]{
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
        'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
        'y', 'z'
    };

    public static int anonymousCounter = 0;
    public static String anonymousName() {
        var sb = new StringBuilder();

        var tmp = ++anonymousCounter;
        while(tmp > letters.length-1) {
            sb.append(letters[tmp % (letters.length-1)]);
            tmp -= letters.length-1;
        }
        sb.append(letters[tmp]);
        return sb.toString();
    }

    public static Value.LocalVariable localVariable() {
        return new Value.LocalVariable(anonymousName());
    }

    public static Value.GlobalVariable globalVariable() {
        return new Value.GlobalVariable(anonymousName());
    }

    public static Instruction.Label label() {
        return new Instruction.Label(anonymousName());
    }

    public static Value.GlobalVariable globalVariable(String name) {
        return new Value.GlobalVariable(name);
    }
}
