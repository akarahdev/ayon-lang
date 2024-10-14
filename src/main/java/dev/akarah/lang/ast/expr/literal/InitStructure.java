package dev.akarah.lang.ast.expr.literal;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Constant;
import dev.akarah.llvm.inst.Types;
import dev.akarah.llvm.inst.Value;
import dev.akarah.llvm.inst.misc.Call;
import dev.akarah.util.Mutable;
import dev.akarah.util.ReferenceCountingLibrary;

import java.util.List;

public record InitStructure(Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        var ptr = transformer.basicBlocks.peek().call(
            Types.pointerTo(Types.VOID),
            new Value.GlobalVariable("malloc"),
            List.of(
                new Call.Parameter(
                    Types.integer(32), Constant.constant(this.type().get().size(this.errorSpan()))))
        );
        transformer.basicBlocks.peek().store(
            this.type().get().llvm(this.errorSpan()),
            new Value.ZeroInitializer(),
            ptr
        );
        ReferenceCountingLibrary.debugPrint(transformer.basicBlocks.peek(), transformer.module, "Allocating refcounted memory");
        return ptr;
    }

    @Override
    public String toString() {
        return "(init " + this.type + ")";
    }
}
