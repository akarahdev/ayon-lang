package dev.akarah.lang.ast.expr;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.util.Mutable;

public record BitCast(Expression expr, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        expr.accept(visitor);
        visitor.expression(this);
    }
}
