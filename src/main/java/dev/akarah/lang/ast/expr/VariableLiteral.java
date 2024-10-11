package dev.akarah.lang.ast.expr;

import dev.akarah.lang.SpanData;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

public record VariableLiteral(String name, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(this);
    }
}
