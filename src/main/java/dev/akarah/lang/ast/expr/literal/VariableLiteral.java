package dev.akarah.lang.ast.expr.literal;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

public record VariableLiteral(String name, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        if (dereferenceLocals) {
            return transformer.basicBlocks.peek().load(
                this.type().get().llvm(this.errorSpan()),
                codeBlock.data().llvmVariables().get(this.name())
            );
        } else {
            return codeBlock.data().llvmVariables().get(this.name());
        }
    }

    @Override
    public String toString() {
        return "(" + name + " as " + type + ")";
    }
}
