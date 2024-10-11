package dev.akarah.lang.ast;

import dev.akarah.lang.SpanData;
import dev.akarah.llvm.inst.Types;

public sealed interface Type {
    record Integer(int width) implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm(SpanData span) {
            return Types.integer(width);
        }

        @Override
        public long size(SpanData span) {
            return width / 8;
        }
    }

    record UnsignedInteger(int width) implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm(SpanData span) {
            return Types.integer(width);
        }

        @Override
        public long size(SpanData span) {
            return width / 8;
        }
    }

    record F32() implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm(SpanData span) {
            return Types.float32();
        }

        @Override
        public long size(SpanData span) {
            return 4;
        }
    }

    record F64() implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm(SpanData span) {
            return Types.float64();
        }

        @Override
        public long size(SpanData span) {
            return 8;
        }
    }

    record F128() implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm(SpanData span) {
            return Types.float128();
        }

        @Override
        public long size(SpanData span) {
            return 16;
        }
    }

    record Unit() implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm(SpanData span) {
            return Types.VOID;
        }

        @Override
        public long size(SpanData span) {
            return 0;
        }
    }

    record CArray(Type type, long length) implements Type  {
        @Override
        public dev.akarah.llvm.inst.Type llvm(SpanData span) {
            return Types.array((int) length, type.llvm(span));
        }

        @Override
        public long size(SpanData span) {
            return length * type.size(span);
        }
    }

    record CStringPointer(Type type) implements Type  {
        @Override
        public dev.akarah.llvm.inst.Type llvm(SpanData span) {
            return Types.pointerTo(type.llvm(span));
        }

        @Override
        public long size(SpanData span) {
            return 4;
        }
    }

    record UserStructure(String name) implements Type {
        @Override
        public dev.akarah.llvm.inst.Type llvm(SpanData span) {
            return ProgramTypeInformation.resolveStructure(name, span).llvm();
        }

        @Override
        public long size(SpanData span) {
            return ProgramTypeInformation
                .resolveStructure(name, span)
                .parameters().values().stream()
                .mapToInt(it -> Math.toIntExact(it.size(span)))
                .sum() + 2;
        }
    }

    dev.akarah.llvm.inst.Type llvm(SpanData span);
    long size(SpanData span);
}
