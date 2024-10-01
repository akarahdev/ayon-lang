package dev.akarah.lang.tree;

public sealed interface Type {
    record Integer(int width) implements Type {
        @Override
        public dev.akarah.ir.Type toLLVMType() {
            return new dev.akarah.ir.Type.Integer(width);
        }
    }
    record UnsignedInteger(int width) implements Type {
        @Override
        public dev.akarah.ir.Type toLLVMType() {
            return new dev.akarah.ir.Type.Integer(width);
        }
    }

    record F32() implements Type {
        @Override
        public dev.akarah.ir.Type toLLVMType() {
            return new dev.akarah.ir.Type.F32();
        }
    }

    record F64() implements Type {
        @Override
        public dev.akarah.ir.Type toLLVMType() {
            return new dev.akarah.ir.Type.F64();
        }
    }

    record F128() implements Type {
        @Override
        public dev.akarah.ir.Type toLLVMType() {
            return new dev.akarah.ir.Type.F128();
        }
    }

    record Unit() implements Type {
        @Override
        public dev.akarah.ir.Type toLLVMType() {
            return new dev.akarah.ir.Type.Void();
        }
    }

    record Array(Type type, long length) implements Type  {
        @Override
        public dev.akarah.ir.Type toLLVMType() {
            return new dev.akarah.ir.Type.Array(type.toLLVMType(), length);
        }
    }

    record Reference(Type type) implements Type  {
        @Override
        public dev.akarah.ir.Type toLLVMType() {
            return new dev.akarah.ir.Type.Ptr(type.toLLVMType());
        }
    }

    public dev.akarah.ir.Type toLLVMType();
}
