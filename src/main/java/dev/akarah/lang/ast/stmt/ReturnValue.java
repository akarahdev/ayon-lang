package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Types;
import dev.akarah.llvm.inst.Value;
import dev.akarah.llvm.inst.misc.Call;
import dev.akarah.util.ReferenceCountingLibrary;

import java.util.List;

public record ReturnValue(
    Expression value,
    SpanData errorSpan
) implements Statement {
    @Override
    public void accept(AST.Visitor visitor) {
        value.accept(visitor);
        visitor.statement(this);
    }

    @Override
    public void llvm(CodeBlock codeBlock, FunctionTransformer transformer) {
        var e = transformer.buildExpression(this.value(), codeBlock, true);
        if(this.value.type().get().isRecord()) {
            transformer.basicBlocks.peek().call(
                Types.integer(16),
                ReferenceCountingLibrary.INCREMENT_REFERENCE_COUNT,
                List.of(new Call.Parameter(
                    Types.pointerTo(Types.VOID),
                    e
                ))
            );
        }
        for (var variableName : codeBlock.data().localVariables().keySet()) {
            if (codeBlock.data().localVariables().get(variableName) instanceof dev.akarah.lang.ast.Type.UserStructure) {
                var loaded = transformer.basicBlocks.peek().load(
                    Types.pointer(),
                    codeBlock.data().llvmVariables().get(variableName)
                );
                transformer.basicBlocks.peek().call(
                    Types.integer(16),
                    ReferenceCountingLibrary.DECREMENT_REFERENCE_COUNT,
                    List.of(new Call.Parameter(
                        Types.pointerTo(Types.VOID),
                        loaded
                    ))
                );
            }
        }
        for(var alloc : codeBlock.data().extraAllocations()) {
            transformer.basicBlocks.peek().call(
                Types.integer(16),
                ReferenceCountingLibrary.DECREMENT_REFERENCE_COUNT,
                List.of(new Call.Parameter(
                    Types.pointerTo(Types.VOID),
                    alloc
                ))
            );
        }
        transformer.basicBlocks.peek().ret(
            this.value().type().get().llvm(this.errorSpan()),
            e
        );
    }

    @Override
    public String toString() {
        return "return " + value;
    }
}
