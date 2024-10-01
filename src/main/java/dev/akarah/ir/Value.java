package dev.akarah.ir;

import java.util.List;
import java.util.stream.Collectors;

public sealed interface Value {
    record IntConstant(long constant) implements Value {
        @Override
        public String toString() {
            return String.valueOf(constant);
        }
    }
    record FloatConstant(double constant) implements Value {
        @Override
        public String toString() {
            return String.valueOf(constant);
        }
    }
    record ArrayConstant(List<Value> values, Type arrayType) implements Value {
        @Override
        public String toString() {
            return "[ " + values.stream().map(it -> arrayType + " " + it.toString())
                .collect(Collectors.joining(", ")) + "]";
        }
    }
    record StructureConstant(List<Value> values, List<Type> types) implements Value {
        @Override
        public String toString() {
            return "TODO";
        }
    }
    record LocalVariable(String name) implements Value {
        @Override
        public String toString() {
            return "%" + name;
        }
    }
    record GlobalVariable(String name) implements Value {
        @Override
        public String toString() {
            return "@" + name;
        }
    }
    record BlockAddress(GlobalVariable function, LocalVariable label) implements Value {
        @Override
        public String toString() {
            return "blockaddress(" + function + ", " + label + ")";
        }
    }
    record Undef() implements Value {
        @Override
        public String toString() {
            return "undef";
        }
    }
    record Poison() implements Value {
        @Override
        public String toString() {
            return "poison";
        }
    }
}
