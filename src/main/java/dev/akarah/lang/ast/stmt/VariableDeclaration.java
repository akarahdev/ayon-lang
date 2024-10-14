package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Types;
import dev.akarah.llvm.inst.Value;
import dev.akarah.llvm.inst.memory.Alloca;
import dev.akarah.llvm.inst.misc.Call;
import dev.akarah.util.Mutable;
import dev.akarah.util.ReferenceCountingLibrary;

import java.util.List;

public record VariableDeclaration(
    String name,
    Mutable<Type> type,
    Expression value,
    SpanData errorSpan
) implements Statement {

    @Override
    public void accept(Visitor visitor) {
        value.accept(visitor);
        visitor.statement(this);
    }

    @Override
    public void llvm(CodeBlock codeBlock, FunctionTransformer transformer) {
        var local = new Value.LocalVariable(
            Value.LocalVariable.random().name()
                + ".local."
                + this.name().replace("::", "."));
        transformer.basicBlocks.peek().perform(new Alloca(
            local,
            this.type().get().llvm(this.errorSpan()),
            1
        ));
        codeBlock.data().llvmVariables().put(this.name(), local);
        var expr = this.value().llvm(codeBlock, true, transformer);
        transformer.basicBlocks.peek().call(
            Types.integer(16),
            ReferenceCountingLibrary.INCREMENT_REFERENCE_COUNT,
            List.of(new Call.Parameter(Types.pointer(),
                expr
            ))
        );
        transformer.basicBlocks.peek().store(this.type().get().llvm(this.errorSpan()), expr, local);
    }

    @Override
    public String toString() {
        return "var " + name + ": " + type + " = " + value;
    }
}
