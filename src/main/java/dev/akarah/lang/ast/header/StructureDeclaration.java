package dev.akarah.lang.ast.header;

import dev.akarah.lang.ast.Type;

import java.util.List;
import java.util.TreeMap;

public record StructureDeclaration(
    String name,
    TreeMap<String, Type> parameters,
    List<Attribute> attributes
) implements Header {
    public void visit(Visitor visitor) {
        visitor.header(this);
    }
}
