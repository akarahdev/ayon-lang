package dev.akarah.lang.ast.header;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.error.CompileError;
import dev.akarah.util.Mutable;

import java.util.LinkedHashMap;
import java.util.List;

public record StructureDeclaration(
    String name,
    LinkedHashMap<String, Type> parameters,
    List<Attribute> attributes,
    SpanData errorSpan,
    Mutable<Integer> cachedSize,
    Mutable<dev.akarah.llvm.inst.Type> cachedLlvmType
) implements Header {
    public void accept(Visitor visitor) {
        visitor.header(this);
    }

    public dev.akarah.llvm.inst.Type llvmPtr() {
        return new dev.akarah.llvm.inst.Type.Ptr();
    }

    public dev.akarah.llvm.inst.Type llvmStruct() {
        if(this.cachedLlvmType().get() != null)
            return this.cachedLlvmType().get();
        var types = new dev.akarah.llvm.inst.Type[this.parameters().size()+1];
        this.cachedLlvmType().set(new dev.akarah.llvm.inst.Type.Structure(types));
        try {
            int index = 1;
            types[0] = new dev.akarah.llvm.inst.Type.Integer(16);
            for(var value : parameters.values()) {
                types[index++] = value.llvm(this.errorSpan());
            }
            return this.cachedLlvmType().get();
        } catch (StackOverflowError stackOverflowError) {
            throw new CompileError.RawMessage("cyclic dependency found", this.errorSpan());
        }
    }
}
