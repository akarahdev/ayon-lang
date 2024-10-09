package dev.akarah.lang.ast.expr;

import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.util.List;

public record CodeBlock(List<Statement> statements, Mutable<Type> type) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        for (var stmt : statements)
            stmt.accept(visitor);
        visitor.expression(this);
    }
}
