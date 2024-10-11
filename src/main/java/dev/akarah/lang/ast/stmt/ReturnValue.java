package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.expr.Expression;

public record ReturnValue(
    Expression value,
    SpanData errorSpan
) implements Statement {
    @Override
    public void accept(AST.Visitor visitor) {
        value.accept(visitor);
        visitor.statement(this);
    }
}
