package dev.akarah.lang.ast.expr.unop;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.util.Mutable;

public record BitCast(Expression expr, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        expr.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        return transformer.basicBlocks.peek().bitcast(
            this.expr().type().get().llvm(this.errorSpan()),
            transformer.buildExpression(this.expr(), codeBlock, true),
            this.type().get().llvm(this.errorSpan())
        );
    }

    @Override
    public String toString() {
        return expr + " as " + type;
    }
}
