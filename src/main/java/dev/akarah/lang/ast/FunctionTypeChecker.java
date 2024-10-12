package dev.akarah.lang.ast;

import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.*;
import dev.akarah.lang.ast.expr.binop.Add;
import dev.akarah.lang.ast.expr.binop.Div;
import dev.akarah.lang.ast.expr.binop.Mul;
import dev.akarah.lang.ast.expr.binop.Sub;
import dev.akarah.lang.ast.expr.literal.VariableLiteral;
import dev.akarah.lang.ast.header.Function;
import dev.akarah.lang.ast.header.Header;
import dev.akarah.lang.ast.stmt.ReturnValue;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.lang.error.CompileError;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

public class FunctionTypeChecker implements AST.Visitor {
    Function function;

    Stack<CodeBlock> codeBlocks = new Stack<>();

    @Override
    public void header(Header header) {
        if(header instanceof Function function2) {
            this.function = function2;
        }
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
        System.out.println("Stmt: " + statement);
        switch (statement) {
            case ReturnValue returnValue -> {
                returnValue.value().accept(this);
                if(!returnValue.value().type().get().equals(function.returnType())
                && !(returnValue.value().type().get() instanceof Type.UserStructure us && us.name().equals("std::any"))
                    && !(function.returnType() instanceof Type.UserStructure us2 && us2.name().equals("std::any"))) {
                    throw new CompileError.RawMessage("return type doesn't match functions' return type", returnValue.value().errorSpan());
                }
            }
            default -> {}
        }
    }

    @Override
    public void expression(Expression expression) {
        System.out.println("Expr: " + expression);
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
            case Invoke invoke -> {
                switch (invoke.base()) {
                    case VariableLiteral variableLiteral -> {
                        var lookedUpFunction = ProgramTypeInformation.resolveFunction(variableLiteral.name(), variableLiteral.errorSpan());
                        if(lookedUpFunction.parameters().size() > invoke.arguments().size()) {
                            throw new CompileError.RawMessage(
                                "not enough arguments, requires " + lookedUpFunction.parameters().size(),
                                invoke.errorSpan());
                        }
                        if(lookedUpFunction.parameters().size() < invoke.arguments().size()) {
                            throw new CompileError.RawMessage(
                                "too many arguments, requires " + lookedUpFunction.parameters().size(),
                                invoke.errorSpan());
                        }
                        AtomicInteger i = new AtomicInteger();
                        lookedUpFunction.parameters().forEach((name, expected) -> {
                            var found = invoke.arguments().get(i.get());
                            i.getAndIncrement();
                            if(!found.type().get().equals(expected)
                                && !expected.equals(new Type.UserStructure("std::any"))
                                && !found.type().get().equals(new Type.UserStructure("std::any"))) {
                                throw new CompileError.RawMessage("expected " + expected + ", found " + found, found.errorSpan());
                            }
                        });
                    }
                    default -> throw new CompileError.RawMessage("currently unsupported", invoke.base().errorSpan());
                }
            }
            default -> {}
        }
    }
}
