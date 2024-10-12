package dev.akarah.lang.ast.expr.binop;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.util.Mutable;

public record Store(Expression lhs, Expression rhs, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        lhs.accept(visitor);
        rhs.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        var ref = transformer.buildExpression(this.lhs(), codeBlock, false);
        transformer.basicBlocks.peek().store(
            this.lhs().type().get().llvm(this.errorSpan()),
            transformer.buildExpression(this.rhs(), codeBlock, true),
            ref
        );
        return null;
    }

    @Override
    public String toString() {
        return lhs + " = " + rhs;
    }
}
