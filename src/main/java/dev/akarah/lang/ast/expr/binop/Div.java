package dev.akarah.lang.ast.expr.binop;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.error.CompileError;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.llvm.inst.misc.Call;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.util.List;

public record Div(Expression lhs, Expression rhs, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        lhs.accept(visitor);
        rhs.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        if(this.type.get().isInteger()) {
            return transformer.basicBlocks.peek().sdiv(
                this.type.get().llvm(this.errorSpan),
                lhs.llvm(codeBlock, true, transformer),
                rhs.llvm(codeBlock, true, transformer)
            );
        } else if(this.type.get().isFloat()) {
            return transformer.basicBlocks.peek().sdiv(
                this.type.get().llvm(this.errorSpan),
                lhs.llvm(codeBlock, true, transformer),
                rhs.llvm(codeBlock, true, transformer)
            );
        } else if(this.type.get().isRecord()) {
            var functionName = ((Type.UserStructure) this.type.get()).name() + "::div";
            return transformer.basicBlocks.peek().call(
                this.type.get().llvm(this.errorSpan),
                new Value.GlobalVariable(FunctionTransformer.mangle(functionName, this.errorSpan)),
                List.of(
                    new Call.Parameter(lhs.type().get().llvm(errorSpan()), lhs.llvm(codeBlock, true, transformer)),
                    new Call.Parameter(rhs.type().get().llvm(errorSpan()), rhs.llvm(codeBlock, true, transformer))
                )
            );
        } else {
            throw new CompileError.RawMessage("operands don't support '/'", this.errorSpan);
        }
    }

    @Override
    public String toString() {
        return "(" + lhs.toString() + " / " + rhs.toString() + ") as " + type;
    }
}
