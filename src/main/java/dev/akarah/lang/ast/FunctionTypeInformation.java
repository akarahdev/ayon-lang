package dev.akarah.lang.ast;

import dev.akarah.lang.ast.expr.*;
import dev.akarah.lang.ast.header.Function;
import dev.akarah.lang.ast.header.Header;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.lang.ast.stmt.VariableDeclaration;
import dev.akarah.llvm.inst.Value;

import java.util.HashMap;

public class FunctionTypeInformation implements AST.Visitor {
    public HashMap<String, Type> locals = new HashMap<>();
    public HashMap<String, Value> llvmLocals = new HashMap<>();

    @Override
    public void header(Header header) {
        switch (header) {
            case Function function -> {
                for(var param : function.parameters().keySet()) {
                    locals.put(param, function.parameters().get(param));
                }
            }
            default -> {}
        }
    }

    @Override
    public void statement(Statement statement) {
        switch (statement) {
            case VariableDeclaration variableDeclaration -> {
                if(variableDeclaration.type().value != null) {
                    locals.put(variableDeclaration.name(), variableDeclaration.type().value);
                } else {
                    locals.put(variableDeclaration.name(), variableDeclaration.value().type().value);
                }
                variableDeclaration.type().value = locals.get(variableDeclaration.name());
            }
            default -> {}
        }
    }

    @Override
    public void expression(Expression expression) {
        switch (expression) {
            case IntegerLiteral il -> {
                if(il.type().value == null) {
                    if (il.integer() > Integer.MAX_VALUE)
                        il.type().value = new Type.Integer(64);
                    else
                        il.type().value = new Type.Integer(32);
                }
            }
            case Add add -> add.type().value = add.lhs().type().value;
            case ArrayLiteral arrayLiteral ->
                arrayLiteral.type().value =
                    new Type.Array(arrayLiteral.values().getFirst().type().value, (long) arrayLiteral.values().size());
            case CodeBlock codeBlock -> {
            }
            case Div div -> div.type().value = div.lhs().type().value;
            case FloatingLiteral floatingLiteral -> {
                if(floatingLiteral.floating() >= Double.MAX_VALUE) {
                    floatingLiteral.type().value = new Type.F128();
                } else if(floatingLiteral.floating() >= Float.MAX_VALUE) {
                    floatingLiteral.type().value = new Type.F64();
                } else  {
                    floatingLiteral.type().value = new Type.F32();
                }
            }
            case Invoke invoke -> {
                switch (invoke.base()) {
                    case VariableLiteral variableLiteral -> {
                        invoke.type().value = ProgramTypeInformation.functions.get(variableLiteral.name()).returnType();
                    }
                    default -> throw new IllegalStateException("uhhh not available yet sowwy");
                }
            }
            case Mul mul -> mul.type().value = mul.lhs().type().value;
            case Negate negate -> negate.type().value = negate.value().type().value;
            case Sub sub -> sub.type().value = sub.lhs().type().value;
            case Subscript subscript -> {
            }
            case VariableLiteral variableLiteral ->
                variableLiteral.type().value = locals.get(variableLiteral.name());
            case CStringLiteral stringLiteral -> {

            }
            default -> throw new IllegalStateException("Unexpected value: " + expression);
        }
    }
}
