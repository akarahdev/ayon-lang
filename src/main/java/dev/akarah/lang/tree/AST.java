package dev.akarah.lang.tree;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

public sealed interface AST {
    interface Visitor {
        void header(Header header);

        void statement(Statement statement);

        void expression(Expression expression);
    }

    record Program(
        List<Header> headers
    ) implements AST {
        public Program join(Program other) {
            var list = new ArrayList<Header>(headers);
            list.addAll(other.headers);
            return new Program(list);
        }
    }

    sealed interface Header extends AST {
        record Function(
            String name,
            TreeMap<String, Type> parameters,
            Type returnType,
            Expression.CodeBlock codeBlock,
            Mutable<FunctionTypeInformation> codeTypeInformation
        ) implements Header {

            public void visit(AST.Visitor visitor) {
                visitor.header(this);
                codeBlock.accept(visitor);
            }
        }

        record FunctionDeclaration(
            String name,
            TreeMap<String, Type> parameters,
            Type returnType
        ) implements Header {
            public void visit(AST.Visitor visitor) {
                visitor.header(this);
            }
        }
    }

    sealed interface Statement extends AST {
        record VariableDeclaration(
            String name,
            Mutable<Type> type,
            Expression value
        ) implements Statement {

            @Override
            public void accept(Visitor visitor) {
                value.accept(visitor);
                visitor.statement(this);
            }
        }

        record ReturnValue(
            Expression value
        ) implements Statement {
            @Override
            public void accept(Visitor visitor) {
                value.accept(visitor);
                visitor.statement(this);
            }
        }

        record WhileLoop(
            Expression condition,
            Expression runWhile
        ) implements Statement {
            @Override
            public void accept(Visitor visitor) {
                condition.accept(visitor);
                runWhile.accept(visitor);
                visitor.statement(this);
            }
        }

        void accept(Visitor visitor);
    }

    sealed interface Expression extends AST, AST.Statement {
        record CodeBlock(List<Statement> statements, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                for (var stmt : statements)
                    stmt.accept(visitor);
                visitor.expression(this);
            }
        }

        record IntegerLiteral(long integer, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(this);
            }
        }

        record FloatingLiteral(double floating, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(this);
            }
        }

        record VariableLiteral(String name, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(this);
            }
        }

        record CStringLiteral(String contents) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(this);
            }

            @Override
            public Mutable<Type> type() {
                return new Mutable<>(
                    new Type.Reference(new Type.Array(new Type.Integer(8), this.contents().getBytes(StandardCharsets.UTF_8).length+1))
                );
            }
        }

        record ArrayLiteral(List<Expression> values, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                for (var value : values)
                    value.accept(visitor);
                visitor.expression(this);
            }
        }

        record Conditional(Expression condition, Expression ifTrue,
                           Optional<Expression> ifFalse, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                condition.accept(visitor);
                ifTrue.accept(visitor);
                ifFalse.ifPresent(iff -> iff.accept(visitor));
                visitor.expression(this);
            }
        }

        record Add(Expression lhs, Expression rhs, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                lhs.accept(visitor);
                rhs.accept(visitor);
                visitor.expression(this);
            }
        }

        record Sub(Expression lhs, Expression rhs, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                lhs.accept(visitor);
                rhs.accept(visitor);
                visitor.expression(this);
            }
        }

        record Mul(Expression lhs, Expression rhs, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                lhs.accept(visitor);
                rhs.accept(visitor);
                visitor.expression(this);
            }
        }

        record Div(Expression lhs, Expression rhs, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                lhs.accept(visitor);
                rhs.accept(visitor);
                visitor.expression(this);
            }
        }

        record Negate(Expression value, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(value);
                visitor.expression(this);
            }
        }

        record Invoke(Expression base, List<Expression> arguments, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(base);
                for (var arg : arguments)
                    visitor.expression(arg);
                visitor.expression(this);
            }
        }

        record Subscript(Expression expression, Expression subscriptWith, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(expression);
                visitor.expression(subscriptWith);
                visitor.expression(this);
            }
        }

        Mutable<Type> type();

        void accept(Visitor visitor);
    }
}
