package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.ast.AST;

record WhileLoop(
    AST.Expression condition,
    AST.Expression runWhile
) implements Statement {
    @Override
    public void accept(AST.Visitor visitor) {
        condition.accept(visitor);
        runWhile.accept(visitor);
        visitor.statement(this);
    }
}
