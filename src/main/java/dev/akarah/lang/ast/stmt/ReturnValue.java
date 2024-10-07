package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.ast.AST;

record ReturnValue(
    AST.Expression value
) implements Statement {
    @Override
    public void accept(AST.Visitor visitor) {
        value.accept(visitor);
        visitor.statement(this);
    }
}
