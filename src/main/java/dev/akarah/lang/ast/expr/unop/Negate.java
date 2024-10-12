package dev.akarah.lang.ast.expr.unop;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

public record Negate(Expression value, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(value);
        visitor.expression(this);
    }

    @Override
    public String toString() {
        return "-" + value;
    }
}
