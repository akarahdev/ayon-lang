package dev.akarah.lang.ast.header;

import dev.akarah.lang.ast.AST;
import dev.akarah.lang.tree.FunctionTypeInformation;
import dev.akarah.lang.tree.Mutable;
import dev.akarah.lang.tree.Type;

import java.util.TreeMap;

record Function(
    String name,
    TreeMap<String, Type> parameters,
    Type returnType,
    Expression.CodeBlock codeBlock,
    Mutable<FunctionTypeInformation> codeTypeInformation
) implements Header {

    public void visit(AST.Visitor visitor) {
        visitor.header(this);
        codeBlock.accept(visitor);
    }
}