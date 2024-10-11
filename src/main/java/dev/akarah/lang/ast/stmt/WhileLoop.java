package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.expr.Expression;

public record WhileLoop(
    Expression condition,
    Expression runWhile,
    SpanData errorSpan
) implements Statement {
    @Override
    public void accept(AST.Visitor visitor) {
        condition.accept(visitor);
        runWhile.accept(visitor);
        visitor.statement(this);
    }
}
