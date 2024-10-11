package dev.akarah.lang.ast;

import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.*;
import dev.akarah.lang.ast.header.Function;
import dev.akarah.lang.ast.header.Header;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.lang.ast.stmt.VariableDeclaration;
import dev.akarah.lang.error.CompileError;

import java.util.Stack;

public class FunctionTypeAnnotator implements AST.Visitor {
    Stack<CodeBlock> codeBlocks = new Stack<>();

    @Override
    public void header(Header header) {
        switch (header) {
            case Function function -> {
                for (var param : function.parameters().keySet()) {
                    function.codeBlock().data().localVariables()
                        .put(param, function.parameters().get(param));
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
                switch (invoke.base()) {
                    case VariableLiteral variableLiteral -> {
                        invoke.type().value = ProgramTypeInformation.resolveFunction(variableLiteral.name(), variableLiteral.errorSpan()).returnType();
                    }
                    default -> throw new IllegalStateException("uhhh not available yet sowwy");
                }
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

                } else {
                    throw new CompileError.RawMessage("unable to resolve type of `" + variableLiteral.name() + "`", variableLiteral.errorSpan());
                }

            }
            case CStringLiteral stringLiteral -> {}
            case BitCast bitCast -> {}
            case InitStructure initStructure -> {
                initStructure.type().set(initStructure.type().get());
            }
            case FieldAccess fieldAccess -> {
                this.expression(fieldAccess.expr());
                var structure = (Type.UserStructure) fieldAccess.expr().type().get();
                var resolved = ProgramTypeInformation.resolveStructure(structure.name(), fieldAccess.errorSpan());
                var outputType = resolved.parameters().get(fieldAccess.field());
                fieldAccess.type().set(outputType);
            }
            case Store store -> {

            }
            default -> throw new IllegalStateException("Unexpected value: " + expression);
        }
    }
}
