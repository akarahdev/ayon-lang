package dev.akarah.lang.ast.header;

import dev.akarah.lang.ast.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

public record StructureDeclaration(
    String name,
    LinkedHashMap<String, Type> parameters,
    List<Attribute> attributes
) implements Header {
    public void visit(Visitor visitor) {
        visitor.header(this);
    }

    public dev.akarah.llvm.inst.Type llvm() {
        var types = new dev.akarah.llvm.inst.Type[this.parameters().size()+1];
        int index = 1;
        types[0] = new dev.akarah.llvm.inst.Type.Integer(16);
        for(var value : parameters.values()) {
            types[index++] = value.llvm();
        }
        return new dev.akarah.llvm.inst.Type.Ptr();
    }
}
