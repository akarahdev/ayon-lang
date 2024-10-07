package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.ast.AST;
import dev.akarah.lang.tree.Mutable;
import dev.akarah.lang.tree.Type;

record VariableDeclaration(
    String name,
    Mutable<Type> type,
    AST.Expression value
) implements Statement {

    @Override
    public void accept(AST.Visitor visitor) {
        value.accept(visitor);
        visitor.statement(this);
    }
}
