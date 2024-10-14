package dev.akarah.lang.ast.expr.binop;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.cfg.BasicBlock;
import dev.akarah.llvm.inst.Constant;
import dev.akarah.llvm.inst.Types;
import dev.akarah.llvm.inst.Value;
import dev.akarah.llvm.inst.misc.Call;
import dev.akarah.llvm.inst.ops.ComparisonOperation;
import dev.akarah.util.Mutable;
import dev.akarah.util.ReferenceCountingLibrary;

import java.util.List;

public record Store(Expression lhs, Expression rhs, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        lhs.accept(visitor);
        rhs.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        var ref = transformer.buildExpression(this.lhs(), codeBlock, false);
        var rhs = transformer.buildExpression(this.rhs(), codeBlock, true);

        if(this.rhs().type().get() instanceof Type.UserStructure structure) {
            var outputBlock = BasicBlock.of(Value.LocalVariable.random());
            ReferenceCountingLibrary.debugPrint(transformer.basicBlocks.peek(), transformer.module, "checking refs");
            transformer.basicBlocks.peek().ifThenElse(
                transformer.basicBlocks.peek().icmp(
                    ComparisonOperation.EQUAL,
                    Types.integer(64),
                    transformer.basicBlocks.peek().ptrToInt(Types.pointer(), ref, Types.integer(64)),
                    transformer.basicBlocks.peek().ptrToInt(Types.pointer(), new Value.NullPtr(), Types.integer(64))
                ),
                ifTrue -> {
                    ReferenceCountingLibrary.debugPrint(ifTrue, transformer.module, "true! continuing onwards");
                    ifTrue.br(outputBlock.name());
                },
                ifFalse -> {
                    transformer.basicBlocks.push(ifFalse);
                    ReferenceCountingLibrary.debugPrint(ifFalse, transformer.module,
                        "releasing "
                            + this.rhs().toString() + " from old allocation");
                    ReferenceCountingLibrary.releasePointer(
                        codeBlock,
                        transformer,
                        structure,
                        transformer.basicBlocks.peek().load(
                            Types.pointer(),
                            ref
                        ),
                        this.errorSpan()
                    );
                    transformer.basicBlocks.pop();
                    ifFalse.br(outputBlock.name());
                }
            );
            transformer.basicBlocks.peek().childBlock(outputBlock);
            transformer.basicBlocks.push(outputBlock);
            transformer.basicBlocks.peek().call(
                Types.integer(16),
                ReferenceCountingLibrary.INCREMENT_REFERENCE_COUNT,
                List.of(new Call.Parameter(Types.pointer(), rhs))
            );
        }

        transformer.basicBlocks.peek().store(
            this.lhs().type().get().llvm(this.errorSpan()),
            rhs,
            ref
        );
        return null;
    }

    @Override
    public String toString() {
        return lhs + " = " + rhs;
    }
}
