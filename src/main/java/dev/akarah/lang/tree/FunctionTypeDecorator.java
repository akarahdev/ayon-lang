package dev.akarah.lang.tree;

import java.util.HashMap;

public class FunctionTypeDecorator implements AST.Visitor {
    public HashMap<String, AST.Header.Function> functions = new HashMap<>();
    public HashMap<String, Type> locals = new HashMap<>();

    @Override
    public void header(AST.Header header) {

    }

    @Override
    public void statement(AST.Statement statement) {
        switch (statement) {
            case AST.Statement.VariableDeclaration variableDeclaration -> {
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
    public void expression(AST.Expression expression) {
        switch (expression) {
            case AST.Expression.IntegerLiteral il -> {
                if(il.type().value == null) {
                    if (il.integer() > Integer.MAX_VALUE)
                        il.type().value = new Type.Integer(64);
                    else
                        il.type().value = new Type.Integer(32);
                }
            }
            case AST.Expression.Add add -> {
            }
            case AST.Expression.ArrayLiteral arrayLiteral ->
                arrayLiteral.type().value =
                    new Type.Array(arrayLiteral.values().getFirst().type().value, (long) arrayLiteral.values().size());
            case AST.Expression.CodeBlock codeBlock -> {
            }
            case AST.Expression.Conditional conditional -> {
                if(conditional.ifFalse().isPresent()) {
                    conditional.type().value = new Type.Union(
                        conditional.ifTrue().type().value,
                        conditional.ifFalse().get().type().value
                    );
                } else {
                    conditional.type().value = new Type.Union(
                        conditional.ifTrue().type().value,
                        new Type.Unit()
                    );
                }
            }
            case AST.Expression.Div div -> div.type().value = div.lhs().type().value;
            case AST.Expression.FloatingLiteral floatingLiteral -> {
                if(floatingLiteral.floating() >= Double.MAX_VALUE) {
                    floatingLiteral.type().value = new Type.F128();
                } else if(floatingLiteral.floating() >= Float.MAX_VALUE) {
                    floatingLiteral.type().value = new Type.F64();
                } else  {
                    floatingLiteral.type().value = new Type.F32();
                }
            }
            case AST.Expression.Invoke invoke -> {
                switch (invoke.base()) {
                    case AST.Expression.VariableLiteral variableLiteral -> {
                        variableLiteral.type().value = functions.get(variableLiteral.name()).returnType();
                    }
                    default -> throw new IllegalStateException("uhhh not available yet sowwy");
                }
            }
            case AST.Expression.Mul mul -> mul.type().value = mul.lhs().type().value;
            case AST.Expression.Negate negate -> negate.type().value = negate.value().type().value;
            case AST.Expression.Sub sub -> sub.type().value = sub.lhs().type().value;
            case AST.Expression.Subscript subscript -> {
            }
            case AST.Expression.VariableLiteral variableLiteral ->
                variableLiteral.type().value = locals.get(variableLiteral.name());
        }
    }
}
