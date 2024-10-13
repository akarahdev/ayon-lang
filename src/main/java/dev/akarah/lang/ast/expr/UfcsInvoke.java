package dev.akarah.lang.ast.expr;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.literal.VariableLiteral;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.lang.error.CompileError;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.llvm.inst.misc.Call;
import dev.akarah.util.Mutable;

import java.util.ArrayList;
import java.util.List;

public record UfcsInvoke(
    Expression callee,
    Mutable<String> functionName,
    SpanData functionNameSpan,
    List<Expression> arguments,
    Mutable<Type> type,
    SpanData errorSpan
) implements Expression, Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(callee);
        arguments.forEach(it -> it.accept(visitor));
        visitor.statement(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        var newArguments = new ArrayList<Call.Parameter>();
        newArguments.add(new Call.Parameter(
            this.callee().type().get().llvm(this.callee().errorSpan()),
            transformer.buildExpression(this.callee(), codeBlock, true)
        ));
        for (var value : this.arguments()) {
            Value newArg = null;
            newArg = transformer.buildExpression(value, codeBlock, true);
            newArguments.add(new Call.Parameter(
                value.type().get().llvm(value.errorSpan()),
                newArg
            ));
        }
        return transformer.basicBlocks.peek().call(
            this.type().get().llvm(this.errorSpan()),
            new Value.GlobalVariable(FunctionTransformer.mangle(this.functionName().get(), this.functionNameSpan())),
            newArguments
        );
    }

    @Override
    public String toString() {
        return this.callee + "." + this.functionName + "(" + arguments.toString().substring(1, arguments.toString().length()-1) + ")";
    }
}
