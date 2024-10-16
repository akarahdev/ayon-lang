package dev.akarah.lang.ast.header;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.FunctionTypeAnnotator;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

public record Function(
    String name,
    LinkedHashMap<String, Type> parameters,
    Type returnType,
    CodeBlock codeBlock,
    List<Attribute> attributes,
    SpanData errorSpan
) implements Header {
    public void accept(AST.Visitor visitor) {
        visitor.header(this);
        codeBlock.accept(visitor);
    }
}