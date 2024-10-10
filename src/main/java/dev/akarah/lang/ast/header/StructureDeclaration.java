package dev.akarah.lang.ast.header;

import dev.akarah.lang.ast.Type;

import java.util.TreeMap;

public record StructureDeclaration(
    String name,
    TreeMap<String, Type> parameters
) implements Header {
    public void visit(Visitor visitor) {
        visitor.header(this);
    }
}
