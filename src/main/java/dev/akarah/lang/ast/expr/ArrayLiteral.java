package dev.akarah.lang.ast.expr;

import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.util.List;

public record ArrayLiteral(List<Expression> values, Mutable<Type> type) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        for (var value : values)
            value.accept(visitor);
        visitor.expression(this);
    }
}
