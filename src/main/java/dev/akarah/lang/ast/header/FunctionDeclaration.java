package dev.akarah.lang.ast.header;

import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.Type;

import java.util.List;
import java.util.TreeMap;

public record FunctionDeclaration(
    String name,
    TreeMap<String, Type> parameters,
    Type returnType,
    List<Attribute> attributes
) implements Header {
    public void visit(AST.Visitor visitor) {
        visitor.header(this);
    }
}
