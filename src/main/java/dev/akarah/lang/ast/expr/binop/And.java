package dev.akarah.lang.ast.expr.binop;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.util.Mutable;

public record And(Expression lhs, Expression rhs, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        lhs.accept(visitor);
        rhs.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public String toString() {
        return "(" + lhs.toString() + " & " + rhs.toString() + ") as " + type;
    }
}
