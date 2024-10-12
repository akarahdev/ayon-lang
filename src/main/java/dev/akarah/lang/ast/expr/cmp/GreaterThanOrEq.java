package dev.akarah.lang.ast.expr.cmp;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.llvm.inst.ops.ComparisonOperation;
import dev.akarah.util.Mutable;

public record GreaterThanOrEq(Expression lhs, Expression rhs, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        lhs.accept(visitor);
        rhs.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        return transformer.basicBlocks.peek().icmp(
            ComparisonOperation.SIGNED_GREATER_THAN_OR_EQUAL,
            this.lhs().type().get().llvm(this.errorSpan()),
            transformer.buildExpression(this.lhs(), codeBlock, true),
            transformer.buildExpression(this.rhs(), codeBlock, true)
        );
    }

    @Override
    public String toString() {
        return "(" + lhs.toString() + " >= " + rhs.toString() + ")";
    }

    @Override
    public Mutable<Type> type() {
        return new Mutable<>(new Type.Integer(1));
    }
}
