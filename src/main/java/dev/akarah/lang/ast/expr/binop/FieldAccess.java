package dev.akarah.lang.ast.expr.binop;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.util.Mutable;

public record FieldAccess(Expression expr, String field, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        expr.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public String toString() {
        return "(" + expr + "->" + field + " as " + type + ")";
    }
}
