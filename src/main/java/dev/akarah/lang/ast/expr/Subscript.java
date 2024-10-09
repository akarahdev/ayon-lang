package dev.akarah.lang.ast.expr;

import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

public record Subscript(Expression expression, Expression subscriptWith, Mutable<Type> type) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(expression);
        visitor.expression(subscriptWith);
        visitor.expression(this);
    }
}
