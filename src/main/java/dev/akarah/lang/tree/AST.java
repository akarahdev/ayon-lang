package dev.akarah.lang.tree;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

public sealed interface AST {
    record Program(
        List<Header> headers
    ) implements AST {
    }

    sealed interface Header extends AST {
        record Function(
            String name,
            TreeMap<String, Type> parameters,
            Type returnType,
            Expression.CodeBlock codeBlock
        ) implements Header {

        }
    }

    sealed interface Statement extends AST {
        record VariableDeclaration(
            String name,
            Optional<Type> type,
            Expression value
        ) implements Statement {

        }

        record ReturnValue(
            Expression value
        ) implements Statement {

        }

        record WhileLoop(
            Expression condition,
            Expression runWhile
        ) implements Statement {

        }
    }

    sealed interface Expression extends AST, AST.Statement {
        record CodeBlock(List<Statement> statements) implements Expression {

        }

        record IntegerLiteral(long integer, Optional<Type> typeHint) implements Expression {

        }

        record FloatingLiteral(double floating, Optional<Type> typeHint) implements Expression {

        }

        record VariableLiteral(String name) implements Expression {

        }

        record ArrayLiteral(List<Expression> values) implements Expression {

        }

        record Conditional(Expression condition, Expression ifTrue,
                           Optional<Expression> ifFalse) implements Expression {

        }

        record Add(Expression lhs, Expression rhs) implements Expression {

        }

        record Sub(Expression lhs, Expression rhs) implements Expression {

        }

        record Mul(Expression lhs, Expression rhs) implements Expression {

        }

        record Div(Expression lhs, Expression rhs) implements Expression {

        }

        record Negate(Expression value) implements Expression {

        }

        record Invoke(Expression base, List<Expression> arguments) implements Expression {

        }

        record Subscript(Expression expression, Expression subscriptWith) implements Expression {

        }
    }
}
