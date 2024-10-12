package dev.akarah.lang.ast.expr.cmp;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.util.Mutable;

public record EqualTo(Expression lhs, Expression rhs, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        lhs.accept(visitor);
        rhs.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public String toString() {
        return "(" + lhs.toString() + " > " + rhs.toString() + ")";
    }

    @Override
    public Mutable<Type> type() {
        return new Mutable<>(new Type.Integer(1));
    }
}
