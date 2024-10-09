package dev.akarah.lang.ast.header;

import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.expr.CodeBlock;
import dev.akarah.lang.ast.FunctionTypeInformation;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.util.TreeMap;

public record Function(
    String name,
    TreeMap<String, Type> parameters,
    Type returnType,
    CodeBlock codeBlock,
    Mutable<FunctionTypeInformation> codeTypeInformation
) implements Header {

    public void visit(AST.Visitor visitor) {
        visitor.header(this);
        codeBlock.accept(visitor);
    }
}