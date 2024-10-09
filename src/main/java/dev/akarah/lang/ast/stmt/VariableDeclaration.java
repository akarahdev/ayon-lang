package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

public record VariableDeclaration(
    String name,
    Mutable<Type> type,
    Expression value
) implements Statement {

    @Override
    public void accept(Visitor visitor) {
        value.accept(visitor);
        visitor.statement(this);
    }
}
