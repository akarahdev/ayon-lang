package dev.akarah.lang.ast;

import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.*;
import dev.akarah.lang.ast.header.Function;
import dev.akarah.lang.ast.header.Header;
import dev.akarah.lang.ast.stmt.ReturnValue;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.lang.error.CompileError;

import java.util.Stack;

public class FunctionTypeChecker implements AST.Visitor {
    Function function;

    Stack<CodeBlock> codeBlocks = new Stack<>();

    public FunctionTypeChecker(Function function) {
        this.function = function;
    }

    @Override
    public void header(Header header) {

    }

    @Override
    public void beginCodeBlock(CodeBlock codeBlock) {
        codeBlocks.add(codeBlock);
    }

    @Override
    public void endCodeBlock(CodeBlock codeBlock) {
        codeBlocks.remove(codeBlock);
    }

    @Override
    public void statement(Statement statement) {
        switch (statement) {
            case ReturnValue returnValue -> {
                if(!returnValue.value().type().get().equals(function.returnType())) {
                    throw new CompileError.RawMessage("return type doesn't match functions' return type", returnValue.value().errorSpan());
                }
            }
            default -> {}
        }
    }

    @Override
    public void expression(Expression expression) {
        switch (expression) {
            case Add binOp -> {
                if(!binOp.lhs().type().get().equals(binOp.rhs().type().get()))
                    throw new CompileError.RawMessage("types in `+` must be the same on both sides", binOp.errorSpan());
            }
            case Sub binOp -> {
                if(!binOp.lhs().type().get().equals(binOp.rhs().type().get()))
                    throw new CompileError.RawMessage("types in `-` must be the same on both sides", binOp.errorSpan());
            }
            case Mul binOp -> {
                if(!binOp.lhs().type().get().equals(binOp.rhs().type().get()))
                    throw new CompileError.RawMessage("types in `*` must be the same on both sides", binOp.errorSpan());
            }
            case Div binOp -> {
                if(!binOp.lhs().type().get().equals(binOp.rhs().type().get()))
                    throw new CompileError.RawMessage("types in `/` must be the same on both sides", binOp.errorSpan());
            }
            default -> {}
        }
    }
}
