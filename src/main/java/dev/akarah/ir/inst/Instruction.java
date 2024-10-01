package dev.akarah.ir.inst;

import dev.akarah.ir.Type;
import dev.akarah.ir.Value;

public sealed interface Instruction {
    // Control Flow
    record Ret(Type type, Value value) implements Instruction {
        @Override
        public String toString() {
            return "ret " + type + " " + value;
        }
    }
    record Unreachable() implements Instruction {}

    record Label(String name) implements Instruction {
        @Override
        public String toString() {
            return name + ":";
        }
    }

    // Operations
    record Neg(Value.LocalVariable output, Type type, Value value) implements Instruction {
        @Override
        public String toString() {
            return output + " = neg " + type + " " + value;
        }
    }
    record FNeg(Value.LocalVariable output, Type type, Value value) implements Instruction {
        @Override
        public String toString() {
            return output + " = fneg " + type + " " + value;
        }
    }
    record Add(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = add " + type + " " + lhs + ", " + rhs;
        }
    }
    record FAdd(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = fadd " + type + " " + lhs + ", " + rhs;
        }
    }
    record Sub(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = sub " + type + " " + lhs + ", " + rhs;
        }
    }
    record FSub(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = fsub " + type + " " + lhs + ", " + rhs;
        }
    }
    record Mul(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = mul " + type + " " + lhs + ", " + rhs;
        }
    }
    record FMul(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = fmul " + type + " " + lhs + ", " + rhs;
        }
    }
    record UDiv(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = udiv " + type + " " + lhs + ", " + rhs;
        }
    }
    record SDiv(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = sdiv " + type + " " + lhs + ", " + rhs;
        }
    }
    record FDiv(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = fdiv " + type + " " + lhs + ", " + rhs;
        }
    }
    record URem(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = urem " + type + " " + lhs + ", " + rhs;
        }
    }
    record SRem(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = srem " + type + " " + lhs + ", " + rhs;
        }
    }
    record FRem(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = frem " + type + " " + lhs + ", " + rhs;
        }
    }
    record Shl(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = shl " + type + " " + lhs + ", " + rhs;
        }
    }
    record Shr(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = shr " + type + " " + lhs + ", " + rhs;
        }
    }
    record And(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = and " + type + " " + lhs + ", " + rhs;
        }
    }
    record Or(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = or " + type + " " + lhs + ", " + rhs;
        }
    }
    record Xor(Value.LocalVariable output, Type type, Value lhs, Value rhs) implements Instruction {
        @Override
        public String toString() {
            return output + " = xor " + type + " " + lhs + ", " + rhs;
        }
    }

    // Memory
    record Alloca(Value.LocalVariable output, Type type) implements Instruction {
        @Override
        public String toString() {
            return output + " = alloca " + type;
        }
    }
    record Load(Value.LocalVariable output, Type type, Value ptr, int align) implements Instruction {
        @Override
        public String toString() {
            return output + " = load " + type + ", ptr " + ptr;
        }
    }
    record Store(Value value, Type type, Value ptr, int align) implements Instruction {
        @Override
        public String toString() {
            return "store " + type + ", ptr " + ptr;
        }
    }


}
