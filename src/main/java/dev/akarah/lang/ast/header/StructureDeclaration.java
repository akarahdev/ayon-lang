package dev.akarah.lang.ast.header;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.error.CompileError;

import java.util.LinkedHashMap;
import java.util.List;

public record StructureDeclaration(
    String name,
    LinkedHashMap<String, Type> parameters,
    List<Attribute> attributes,
    SpanData errorSpan
) implements Header {
    public void visit(Visitor visitor) {
        visitor.header(this);
    }

    public dev.akarah.llvm.inst.Type llvmPtr() {
        try {
            var types = new dev.akarah.llvm.inst.Type[this.parameters().size()+1];
            int index = 1;
            types[0] = new dev.akarah.llvm.inst.Type.Integer(16);
            for(var value : parameters.values()) {
                types[index++] = value.llvm(this.errorSpan());
            }
            return new dev.akarah.llvm.inst.Type.Ptr();
        } catch (StackOverflowError stackOverflowError) {
            throw new CompileError.RawMessage("cyclic dependency found", this.errorSpan());
        }
    }

    public dev.akarah.llvm.inst.Type llvmStruct() {
        try {
            var types = new dev.akarah.llvm.inst.Type[this.parameters().size()+1];
            int index = 1;
            types[0] = new dev.akarah.llvm.inst.Type.Integer(16);
            for(var value : parameters.values()) {
                types[index++] = value.llvm(this.errorSpan());
            }
            return new dev.akarah.llvm.inst.Type.Structure(types);
        } catch (StackOverflowError stackOverflowError) {
            throw new CompileError.RawMessage("cyclic dependency found", this.errorSpan());
        }
    }
}
