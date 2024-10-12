package dev.akarah.lang.ast.expr.literal;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.util.Mutable;

public record NullLiteral(Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        return new Value.NullPtr();
    }

    @Override
    public String toString() {
        return "nullptr";
    }
}
