package dev.akarah.lang.tree;

public sealed interface Type {
    record Integer(int width) implements Type {

    }
    record UnsignedInteger(int width) implements Type {

    }

    record F32() implements Type {

    }

    record F64() implements Type {

    }

    record F128() implements Type {

    }

    record Unit() implements Type {

    }

    record Array(Type type, long length) implements Type  {

    }

    record Reference(Type type) implements Type  {

    }
}
