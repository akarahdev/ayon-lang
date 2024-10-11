package dev.akarah.lang.ast;

import dev.akarah.llvm.inst.Types;

public sealed interface Type {
    record Integer(int width) implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm() {
            return Types.integer(width);
        }

        @Override
        public long size() {
            return width / 8;
        }
    }

    record UnsignedInteger(int width) implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm() {
            return Types.integer(width);
        }

        @Override
        public long size() {
            return width / 8;
        }
    }

    record F32() implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm() {
            return Types.float32();
        }

        @Override
        public long size() {
            return 4;
        }
    }

    record F64() implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm() {
            return Types.float64();
        }

        @Override
        public long size() {
            return 8;
        }
    }

    record F128() implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm() {
            return Types.float128();
        }

        @Override
        public long size() {
            return 16;
        }
    }

    record Unit() implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm() {
            return Types.VOID;
        }

        @Override
        public long size() {
            return 0;
        }
    }

    record CArray(Type type, long length) implements Type  {
        @Override
        public dev.akarah.llvm.inst.Type llvm() {
            return Types.array((int) length, type.llvm());
        }

        @Override
        public long size() {
            return length * type.size();
        }
    }

    record CStringPointer(Type type) implements Type  {
        @Override
        public dev.akarah.llvm.inst.Type llvm() {
            return Types.pointerTo(type.llvm());
        }

        @Override
        public long size() {
            return 4;
        }
    }

    record UserStructure(String name) implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm() {
            return ProgramTypeInformation.resolveStructure(name).llvm();
        }

        @Override
        public long size() { return ProgramTypeInformation.resolveStructure(name).parameters().values().stream().mapToInt(it -> Math.toIntExact(it.size())).sum() + 2; }
    }

    dev.akarah.llvm.inst.Type llvm();
    long size();
}
