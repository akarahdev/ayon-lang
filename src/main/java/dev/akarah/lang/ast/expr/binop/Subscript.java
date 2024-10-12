package dev.akarah.lang.ast.expr.binop;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.error.CompileError;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

public record Subscript(Expression expression, Expression subscriptWith, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(expression);
        visitor.expression(subscriptWith);
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        throw new CompileError.RawMessage("not supported yet", this.errorSpan);
    }

    @Override
    public String toString() {
        return expression + "[" + subscriptWith + "]";
    }
}
