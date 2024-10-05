package dev.akarah.lang.tree;

import dev.akarah.llvm.inst.Value;

import java.util.HashMap;

public class FunctionTypeInformation implements AST.Visitor {
    public HashMap<String, Type> locals = new HashMap<>();
    public HashMap<String, Value> llvmLocals = new HashMap<>();

    @Override
    public void header(AST.Header header) {
        switch (header) {
            case AST.Header.Function function -> {
                for(var param : function.parameters().keySet()) {
                    locals.put(param, function.parameters().get(param));
                }
                System.out.println("LOCALS: " + locals);
            }
            default -> {}
        }
    }

    @Override
    public void statement(AST.Statement statement) {
        switch (statement) {
            case AST.Statement.VariableDeclaration variableDeclaration -> {
                if(variableDeclaration.type().v != null) {
                    locals.put(variableDeclaration.name(), variableDeclaration.type().v);
                } else {
                    locals.put(variableDeclaration.name(), variableDeclaration.value().type().v);
                }
                variableDeclaration.type().v = locals.get(variableDeclaration.name());
            }
            default -> {}
        }
    }

    @Override
    public void expression(AST.Expression expression) {
        System.out.println("Expring: " + expression);
        switch (expression) {
            case AST.Expression.IntegerLiteral il -> {
                if(il.type().v == null) {
                    if (il.integer() > Integer.MAX_VALUE)
                        il.type().v = new Type.Integer(64);
                    else
                        il.type().v = new Type.Integer(32);
                }
            }
            case AST.Expression.Add add -> add.type().v = add.lhs().type().v;
            case AST.Expression.ArrayLiteral arrayLiteral ->
                arrayLiteral.type().v =
                    new Type.Array(arrayLiteral.values().getFirst().type().v, (long) arrayLiteral.values().size());
            case AST.Expression.CodeBlock codeBlock -> {
            }
            case AST.Expression.Conditional conditional -> {
                if(conditional.ifFalse().isPresent()) {
                    conditional.type().v = new Type.Union(
                        conditional.ifTrue().type().v,
                        conditional.ifFalse().get().type().v
                    );
                } else {
                    conditional.type().v = new Type.Union(
                        conditional.ifTrue().type().v,
                        new Type.Unit()
                    );
                }
            }
            case AST.Expression.Div div -> div.type().v = div.lhs().type().v;
            case AST.Expression.FloatingLiteral floatingLiteral -> {
                if(floatingLiteral.floating() >= Double.MAX_VALUE) {
                    floatingLiteral.type().v = new Type.F128();
                } else if(floatingLiteral.floating() >= Float.MAX_VALUE) {
                    floatingLiteral.type().v = new Type.F64();
                } else  {
                    floatingLiteral.type().v = new Type.F32();
                }
            }
            case AST.Expression.Invoke invoke -> {
                switch (invoke.base()) {
                    case AST.Expression.VariableLiteral variableLiteral -> {
                        System.out.println(variableLiteral);
                        invoke.type().v = ProgramTypeInformation.functions.get(variableLiteral.name()).returnType();
                    }
                    default -> throw new IllegalStateException("uhhh not available yet sowwy");
                }
            }
            case AST.Expression.Mul mul -> mul.type().v = mul.lhs().type().v;
            case AST.Expression.Negate negate -> negate.type().v = negate.value().type().v;
            case AST.Expression.Sub sub -> sub.type().v = sub.lhs().type().v;
            case AST.Expression.Subscript subscript -> {
            }
            case AST.Expression.VariableLiteral variableLiteral ->
                variableLiteral.type().v = locals.get(variableLiteral.name());
        }
        System.out.println("Expring 2: " + expression);
    }
}
