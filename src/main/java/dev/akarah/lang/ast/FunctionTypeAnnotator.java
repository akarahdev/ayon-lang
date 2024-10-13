package dev.akarah.lang.ast;

import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.*;
import dev.akarah.lang.ast.expr.binop.*;
import dev.akarah.lang.ast.expr.cmp.*;
import dev.akarah.lang.ast.expr.literal.*;
import dev.akarah.lang.ast.expr.unop.BitCast;
import dev.akarah.lang.ast.expr.unop.Negate;
import dev.akarah.lang.ast.header.Function;
import dev.akarah.lang.ast.header.Header;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.lang.ast.stmt.VariableDeclaration;
import dev.akarah.lang.error.CompileError;

import java.util.Stack;

public class FunctionTypeAnnotator implements AST.Visitor {
    Stack<CodeBlock> codeBlocks = new Stack<>();
    Function function;

    @Override
    public void header(Header header) {
        switch (header) {
            case Function function2 -> {
                this.function = function2;
                for (var param : function2.parameters().keySet()) {
                    function2.codeBlock().data().localVariables()
                        .put(param, function2.parameters().get(param));
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void beginCodeBlock(CodeBlock codeBlock) {
        if(!codeBlocks.isEmpty()) {
            for(var localName : codeBlocks.peek().data().localVariables().keySet()) {
                codeBlock.data().localVariables().put(
                    localName,
                    codeBlocks.peek().data().localVariables().get(localName)
                );
            }
        } else {
            for (var param : function.parameters().keySet()) {
                codeBlock.data().localVariables()
                    .put(param, function.parameters().get(param));
            }
        }
        codeBlocks.add(codeBlock);
    }

    @Override
    public void endCodeBlock(CodeBlock codeBlock) {
        codeBlocks.removeLast();
    }

    @Override
    public void statement(Statement statement) {
        switch (statement) {
            case VariableDeclaration variableDeclaration -> {
                if (variableDeclaration.type().value != null) {
                    codeBlocks.peek().data().localVariables()
                        .put(variableDeclaration.name(), variableDeclaration.type().value);
                } else {
                    codeBlocks.peek().data().localVariables()
                        .put(variableDeclaration.name(), variableDeclaration.value().type().value);
                }
                variableDeclaration.type().value =
                    codeBlocks.peek().data().localVariables()
                        .get(variableDeclaration.name());
            }
            case Expression expression -> this.expression(expression);
            default -> {

            }
        }
    }

    @Override
    public void expression(Expression expression) {
        switch (expression) {
            case IntegerLiteral il -> {
                if (il.type().value == null) {
                    if (il.integer() > Integer.MAX_VALUE)
                        il.type().value = new Type.Integer(64);
                    else
                        il.type().value = new Type.Integer(32);
                }
            }
            case Add add -> add.type().value = add.lhs().type().value;
            case ArrayLiteral arrayLiteral -> arrayLiteral.type().value =
                new Type.CArray(arrayLiteral.values().getFirst().type().value, (long) arrayLiteral.values().size());
            case Div div -> div.type().value = div.lhs().type().value;
            case FloatingLiteral floatingLiteral -> {
                if (floatingLiteral.floating() >= Double.MAX_VALUE) {
                    floatingLiteral.type().value = new Type.F128();
                } else if (floatingLiteral.floating() >= Float.MAX_VALUE) {
                    floatingLiteral.type().value = new Type.F64();
                } else {
                    floatingLiteral.type().value = new Type.F32();
                }
            }
            case Invoke invoke -> {
                for(var arg : invoke.arguments())
                    this.expression(arg);
                switch (invoke.base()) {
                    case VariableLiteral variableLiteral -> {
                        invoke.type().value = ProgramTypeInformation.resolveFunction(variableLiteral.name(), variableLiteral.errorSpan()).returnType();
                    }
                    default -> throw new IllegalStateException("uhhh not available yet sowwy");
                }
            }
            case UfcsInvoke invoke -> {
                for(var arg : invoke.arguments())
                    this.expression(arg);

                try {
                    var resolution = ProgramTypeInformation.resolveFunction(
                        invoke.callee().type().toString() + "::" + invoke.functionName(),
                        invoke.errorSpan()
                    );
                    invoke.functionName().set(resolution.name());
                } catch (CompileError e) {
                    var resolution = ProgramTypeInformation.resolveFunction(
                        invoke.functionName().get(),
                        invoke.errorSpan()
                    );
                }
                invoke.type().set(ProgramTypeInformation.resolveFunction(invoke.functionName().get(), invoke.functionNameSpan()).returnType());
            }
            case Mul mul -> mul.type().value = mul.lhs().type().value;
            case Negate negate -> negate.type().value = negate.value().type().value;
            case Sub sub -> sub.type().value = sub.lhs().type().value;
            case Subscript subscript -> {
            }
            case VariableLiteral variableLiteral -> {
                if(codeBlocks.peek().data().localVariables().containsKey(variableLiteral.name())) {
                    variableLiteral.type().value =
                        codeBlocks.peek().data().localVariables()
                            .get(variableLiteral.name());
                } else if(ProgramTypeInformation.headers.containsKey(variableLiteral.name())) {
                    variableLiteral.type().set(new Type.Unit());
                } else {
                    throw new CompileError.RawMessage("unable to resolve type of `" + variableLiteral.name() + "`", variableLiteral.errorSpan());
                }

            }
            case CStringLiteral stringLiteral -> {}
            case StdStringLiteral stdStringLiteral -> {}
            case BitCast bitCast -> {}
            case InitStructure initStructure -> {
                initStructure.type().set(initStructure.type().get());
            }
            case FieldAccess fieldAccess -> {
                fieldAccess.expr().accept(this);
                var structure = (Type.UserStructure) fieldAccess.expr().type().get();
                var resolved = ProgramTypeInformation.resolveStructure(structure.name(), fieldAccess.expr().errorSpan());
                var outputType = resolved.parameters().get(fieldAccess.field());
                fieldAccess.type().set(outputType);
            }
            case Store store -> {

            }
            case GreaterThan op -> op.type().set(op.lhs().type().get());
            case GreaterThanOrEq op -> op.type().set(op.lhs().type().get());
            case LessThan op -> op.type().set(op.lhs().type().get());
            case LessThanOrEq op -> op.type().set(op.lhs().type().get());
            case EqualTo eq -> {}
            case NullLiteral nullLiteral -> nullLiteral.type().set(new Type.UserStructure("std::any"));
            default -> throw new IllegalStateException("Unexpected value: " + expression + "(" + expression.getClass() + ")");
        }
    }
}
