package dev.akarah.lang.ast.expr.unop;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.util.Mutable;

public record Not(Expression value, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        value.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public String toString() {
        return "(!" + value + ") as " + type;
    }
}
