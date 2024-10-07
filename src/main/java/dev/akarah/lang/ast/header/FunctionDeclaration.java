package dev.akarah.lang.ast.header;

import dev.akarah.lang.ast.AST;
import dev.akarah.lang.tree.Type;

import java.util.TreeMap;

record FunctionDeclaration(
    String name,
    TreeMap<String, Type> parameters,
    Type returnType
) implements Header {
    public void visit(AST.Visitor visitor) {
        visitor.header(this);
    }
}
