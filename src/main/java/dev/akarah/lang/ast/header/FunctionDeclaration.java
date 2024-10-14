package dev.akarah.lang.ast.header;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.Type;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

public record FunctionDeclaration(
    String name,
    LinkedHashMap<String, Type> parameters,
    Type returnType,
    List<Attribute> attributes,
    SpanData errorSpan,
    boolean varArgs
) implements Header {
    public void accept(AST.Visitor visitor) {
        visitor.header(this);
    }
}
