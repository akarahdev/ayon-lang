package dev.akarah.lang.ast.expr;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.literal.VariableLiteral;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.lang.error.CompileError;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.llvm.inst.misc.Call;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.util.ArrayList;
import java.util.List;

public record Invoke(Expression base, List<Expression> arguments, Mutable<Type> type, SpanData errorSpan) implements Expression, Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(base);
        arguments.forEach(it -> it.accept(visitor));
        visitor.statement(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        if (this.base() instanceof VariableLiteral variableLiteral) {
            var arguments = new ArrayList<Call.Parameter>();

            for (var value : this.arguments()) {
                Value newArg = null;
                newArg = transformer.buildExpression(value, codeBlock, true);
                arguments.add(new Call.Parameter(
                    value.type().get().llvm(this.errorSpan()),
                    newArg
                ));
            }
            return transformer.basicBlocks.peek().call(
                this.type().get().llvm(this.errorSpan()),
                new Value.GlobalVariable(FunctionTransformer.mangle(variableLiteral.name(), variableLiteral.errorSpan())),
                arguments
            );
        }
        throw new CompileError.RawMessage("calls must be constant", this.errorSpan);
    }

    @Override
    public String toString() {
        return base + "(" + arguments.toString().substring(1, arguments.toString().length()-1) + ")";
    }
}
