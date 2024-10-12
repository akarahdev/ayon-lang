package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.cfg.BasicBlock;
import dev.akarah.llvm.inst.Value;

public record IfStatement(
    Expression condition,
    CodeBlock ifTrue,
    CodeBlock ifFalse,
    SpanData errorSpan
) implements Statement {

    @Override
    public void accept(Visitor visitor) {
        condition.accept(visitor);
        ifTrue.accept(visitor);
        ifFalse.accept(visitor);
        visitor.statement(this);
    }

    @Override
    public void llvm(CodeBlock codeBlock, FunctionTransformer transformer) {
        var condition = this.condition().llvm(codeBlock, true, transformer);
        var finishingBlock = BasicBlock.of(Value.LocalVariable.random());
        transformer.basicBlocks.peek().ifThenElse(
            condition,
            trueBlock -> {
                transformer.basicBlocks.add(trueBlock);
                for (var stmt : this.ifTrue().statements()) {
                    stmt.llvm(codeBlock, transformer);
                }

                if(this.ifTrue().statements().stream().noneMatch(it -> it instanceof ReturnValue)) {
                    transformer.basicBlocks.peek().br(finishingBlock.name());
                }

                transformer.basicBlocks.remove(trueBlock);
            },
            falseBlock -> {
                transformer.basicBlocks.add(falseBlock);
                for (var stmt : this.ifFalse().statements()) {
                    transformer.buildStatement(stmt, codeBlock);
                }


                if(this.ifFalse().statements().stream().noneMatch(it -> it instanceof ReturnValue)) {
                    transformer.basicBlocks.peek().br(finishingBlock.name());
                }

                transformer.basicBlocks.remove(falseBlock);
            }
        );
        transformer.basicBlocks.peek().childBlock(finishingBlock);
        transformer.basicBlocks.add(finishingBlock);
    }
}
