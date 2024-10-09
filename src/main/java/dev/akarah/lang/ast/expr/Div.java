package dev.akarah.lang.ast.expr;

import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

public record Div(Expression lhs, Expression rhs, Mutable<Type> type) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        lhs.accept(visitor);
        rhs.accept(visitor);
        visitor.expression(this);
    }
}
