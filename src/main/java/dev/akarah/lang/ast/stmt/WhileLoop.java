package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.expr.Expression;

record WhileLoop(
    Expression condition,
    Expression runWhile
) implements Statement {
    @Override
    public void accept(AST.Visitor visitor) {
        condition.accept(visitor);
        runWhile.accept(visitor);
        visitor.statement(this);
    }
}
