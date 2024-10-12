package dev.akarah.lang.ast.expr.literal;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.error.CompileError;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.util.List;
import java.util.stream.Collectors;

public record ArrayLiteral(List<Expression> values, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        for (var value : values)
            value.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        throw new CompileError.RawMessage("not supported yet", this.errorSpan);
    }

    @Override
    public String toString() {
        return values.stream().map(Object::toString).collect(Collectors.joining(", \n"));
    }
}
