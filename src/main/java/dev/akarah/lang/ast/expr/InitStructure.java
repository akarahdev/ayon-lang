package dev.akarah.lang.ast.expr;

import dev.akarah.lang.ast.Type;
import dev.akarah.util.Mutable;

public record InitStructure(Mutable<Type> type) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(this);
    }
}
