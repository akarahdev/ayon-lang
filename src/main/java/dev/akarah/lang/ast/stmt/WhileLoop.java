package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.error.CompileError;
import dev.akarah.lang.llvm.FunctionTransformer;

public record WhileLoop(
    Expression condition,
    Expression runWhile,
    SpanData errorSpan
) implements Statement {
    @Override
    public void accept(AST.Visitor visitor) {
        condition.accept(visitor);
        runWhile.accept(visitor);
        visitor.statement(this);
    }

    @Override
    public void llvm(CodeBlock codeBlock, FunctionTransformer transformer) {
        throw new CompileError.RawMessage("not supported yet", this.errorSpan);
    }
}
