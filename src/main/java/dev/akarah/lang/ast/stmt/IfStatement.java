package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;

public record IfStatement(
    Expression condition,
    CodeBlock ifTrue,
    CodeBlock ifFalse,
    SpanData errorSpan
) implements Statement {

    @Override
    public void accept(Visitor visitor) {
        condition.accept(visitor);
        ifTrue.accept(visitor);
        ifFalse.accept(visitor);
        visitor.statement(this);
    }
}
