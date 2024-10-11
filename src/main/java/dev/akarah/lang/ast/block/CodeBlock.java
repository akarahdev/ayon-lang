package dev.akarah.lang.ast.block;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.util.List;

public record CodeBlock(
    List<Statement> statements,
    CodeBlockData data,
    SpanData errorSpan
) implements AST {
    public void accept(AST.Visitor visitor) {
        visitor.beginCodeBlock(this);
        for (var stmt : statements)
            stmt.accept(visitor);
        visitor.endCodeBlock(this);
    }
}
