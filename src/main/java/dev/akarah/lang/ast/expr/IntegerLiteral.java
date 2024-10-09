package dev.akarah.lang.ast.expr;

import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

public record IntegerLiteral(long integer, Mutable<Type> type) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(this);
    }
}
