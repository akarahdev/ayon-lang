package dev.akarah.ir;

import java.util.List;

public sealed interface Type {
    record Void() implements Type {
        @Override
        public String toString() {
            return "void";
        }
    }
    record Integer(int width) implements Type {
        @Override
        public String toString() {
            return "i" + width;
        }
    }
    record F32() implements Type {
        @Override
        public String toString() {
            return "float";
        }
    }
    record F64() implements Type {
        @Override
        public String toString() {
            return "double";
        }
    }
    record F128() implements Type {
        @Override
        public String toString() {
            return "fp128";
        }
    }
    record Ptr(Type pointerTo) implements Type {
        @Override
        public String toString() {
            return "ptr";
        }
    }
    record Vector(Type type, long width) implements Type {
        @Override
        public String toString() {
            return "< " + type + " x " + width + " >";
        }
    }
    record Array(Type type, long length) implements Type {
        @Override
        public String toString() {
            return "[ " + type + " x " + length + " ]";
        }
    }
    record Struct(List<Type> types) implements Type {
        @Override
        public String toString() {
            return types.toString().replace("[", "{ ").replace("]", " }");
        }
    }
}
