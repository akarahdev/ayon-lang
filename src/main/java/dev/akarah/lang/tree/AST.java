package dev.akarah.lang.tree;

import dev.akarah.llvm.cfg.BasicBlock;
import dev.akarah.llvm.inst.Constant;
import dev.akarah.llvm.inst.Value;

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
    }

    sealed interface Header extends AST {
        record Function(
            String name,
            TreeMap<String, Type> parameters,
            Type returnType,
            Expression.CodeBlock codeBlock
        ) implements Header {
            public dev.akarah.llvm.cfg.Function toLLVM() {
                var f = dev.akarah.llvm.cfg.Function.of(new Value.GlobalVariable(this.name));
                for (var key : parameters.keySet()) {
                    f.parameter(parameters.get(key).llvm(), Value.LocalVariable.random());
                }
                f.returns(returnType.llvm());
                f.withBasicBlock(codeBlock.toBasicBlock());
                return f;
            }

            public void visit(AST.Visitor visitor) {
                visitor.header(this);
                codeBlock.accept(visitor);
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

            @Override
            public Value llvm(BasicBlock basicBlock) {
                var variable = basicBlock.alloca(this.type.value.llvm());
                basicBlock.store(this.type.value.llvm(), value.llvm(basicBlock), variable);
                return null;
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

            @Override
            public Value llvm(BasicBlock basicBlock) {
                basicBlock.ret(this.value().type().value.llvm(), this.value.llvm(basicBlock));
                return null;
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

            @Override
            public Value llvm(BasicBlock basicBlock) {
                throw new UnsupportedOperationException("WIP");
            }
        }

        void accept(Visitor visitor);

        Value llvm(BasicBlock basicBlock);
    }

    sealed interface Expression extends AST, AST.Statement {
        record CodeBlock(List<Statement> statements, Mutable<Type> type) implements Expression {
            public BasicBlock toBasicBlock() {
                var bb = BasicBlock.of(Value.LocalVariable.random());
                for (var stmt : statements) {
                    stmt.llvm(bb);
                }
                return bb;
            }

            @Override
            public void accept(Visitor visitor) {
                for (var stmt : statements)
                    stmt.accept(visitor);
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                var bb = this.toBasicBlock();
                basicBlock.br(bb.name());
                return bb.name();
            }
        }

        record IntegerLiteral(long integer, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                return Constant.constant(integer);
            }
        }

        record FloatingLiteral(double floating, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                return Constant.constant(floating);
            }
        }

        record VariableLiteral(String name, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                throw new UnsupportedOperationException("WIP");
            }
        }

        record ArrayLiteral(List<Expression> values, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                for (var value : values)
                    value.accept(visitor);
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                throw new UnsupportedOperationException("WIP");
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

            @Override
            public Value llvm(BasicBlock basicBlock) {
                throw new UnsupportedOperationException("WIP");
            }
        }

        record Add(Expression lhs, Expression rhs, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                lhs.accept(visitor);
                rhs.accept(visitor);
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                return basicBlock.add(this.type.value.llvm(), lhs.llvm(basicBlock), rhs.llvm(basicBlock));
            }
        }

        record Sub(Expression lhs, Expression rhs, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                lhs.accept(visitor);
                rhs.accept(visitor);
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                return basicBlock.sub(this.type.value.llvm(), lhs.llvm(basicBlock), rhs.llvm(basicBlock));
            }
        }

        record Mul(Expression lhs, Expression rhs, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                lhs.accept(visitor);
                rhs.accept(visitor);
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                return basicBlock.mul(this.type.value.llvm(), lhs.llvm(basicBlock), rhs.llvm(basicBlock));
            }
        }

        record Div(Expression lhs, Expression rhs, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                lhs.accept(visitor);
                rhs.accept(visitor);
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                return basicBlock.sdiv(this.type.value.llvm(), lhs.llvm(basicBlock), rhs.llvm(basicBlock));
            }
        }

        record Negate(Expression value, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(value);
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                throw new UnsupportedOperationException("WIP");
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

            @Override
            public Value llvm(BasicBlock basicBlock) {
                switch (this.base) {
                    default -> throw new UnsupportedOperationException("WIP");
                }
            }
        }

        record Subscript(Expression expression, Expression subscriptWith, Mutable<Type> type) implements Expression {
            @Override
            public void accept(Visitor visitor) {
                visitor.expression(expression);
                visitor.expression(subscriptWith);
                visitor.expression(this);
            }

            @Override
            public Value llvm(BasicBlock basicBlock) {
                throw new UnsupportedOperationException("WIP");
            }
        }

        Mutable<Type> type();

        void accept(Visitor visitor);

        Value llvm(BasicBlock basicBlock);
    }
}
