package dev.akarah.lang.ast.expr;

import dev.akarah.lang.SpanData;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.util.List;

public record Invoke(Expression base, List<Expression> arguments, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(base);
        for (var arg : arguments)
            visitor.expression(arg);
        visitor.expression(this);
    }
}
