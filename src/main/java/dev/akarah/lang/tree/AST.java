package dev.akarah.lang.tree;

import dev.akarah.ir.SSABuilder;
import dev.akarah.ir.Value;
import dev.akarah.ir.define.Function;
import dev.akarah.ir.inst.Instruction;

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
            public dev.akarah.ir.define.Function toLLVM() {
                var f = dev.akarah.ir.define.Function.of(
                    new Value.GlobalVariable(this.name), returnType.toLLVMType()
                );
                codeBlock.instructions(f);
                return f;
            }
        }
    }

    sealed interface Statement extends AST {
        record VariableDeclaration(
            String name,
            Optional<Type> type,
            Expression value
        ) implements Statement {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                var l = SSABuilder.localVariable();
                currentFunction.instructions().add(new Instruction.Alloca(l, new dev.akarah.ir.Type.Integer(32)));
                currentFunction.locals().put(this.name, l);
                return l;
            }
        }

        record ReturnValue(
            Expression value
        ) implements Statement {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                var local = value.instructions(currentFunction);
                currentFunction.instructions().add(new Instruction.Ret(new dev.akarah.ir.Type.Integer(32), local));
                return null;
            }
        }

        record WhileLoop(
            Expression condition,
            Expression runWhile
        ) implements Statement {

            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                return null;
            }
        }

        Value.LocalVariable instructions(Function currentFunction);
    }

    sealed interface Expression extends AST, AST.Statement {
        record CodeBlock(List<Statement> statements) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                var cb = SSABuilder.label();
                currentFunction.instructions().add(cb);
                for (var stmt : statements) {
                    stmt.instructions(currentFunction);
                }
                return new Value.LocalVariable(cb.name());
            }
        }

        record IntegerLiteral(long integer, Optional<Type> typeHint) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                var l = SSABuilder.localVariable();
                currentFunction.instructions().add(
                    new Instruction.Add(
                        l,
                        typeHint.orElse(new Type.Integer(32)).toLLVMType(),
                        new Value.IntConstant(integer), new Value.IntConstant(0)));
                return l;
            }
        }

        record FloatingLiteral(double floating, Optional<Type> typeHint) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                var l = SSABuilder.localVariable();
                currentFunction.instructions().add(
                    new Instruction.FAdd(
                        l,
                        typeHint.orElse(new Type.F32()).toLLVMType(),
                        new Value.FloatConstant(floating), new Value.FloatConstant(0)));
                return null;
            }
        }

        record VariableLiteral(String name) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                var l = SSABuilder.localVariable();
                currentFunction.instructions().add(
                    new Instruction.Load(l, new dev.akarah.ir.Type.Integer(32), currentFunction.locals().get(this.name), 4));
                return l;
            }
        }

        record ArrayLiteral(List<Expression> values) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                return null;
            }
        }

        record Conditional(Expression condition, Expression ifTrue,
                           Optional<Expression> ifFalse) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                return null;
            }
        }

        record Add(Expression lhs, Expression rhs) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                var l = SSABuilder.localVariable();
                currentFunction.instructions().add(
                    new Instruction.Add(
                        l,
                        new dev.akarah.ir.Type.Integer(32),
                        lhs.instructions(currentFunction),
                        rhs.instructions(currentFunction)
                    )
                );
                return l;
            }
        }

        record Sub(Expression lhs, Expression rhs) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                var l = SSABuilder.localVariable();
                currentFunction.instructions().add(
                    new Instruction.Sub(
                        l,
                        new dev.akarah.ir.Type.Integer(32),
                        lhs.instructions(currentFunction),
                        rhs.instructions(currentFunction)
                    )
                );
                return l;
            }
        }

        record Mul(Expression lhs, Expression rhs) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                var l = SSABuilder.localVariable();
                currentFunction.instructions().add(
                    new Instruction.Mul(
                        l,
                        new dev.akarah.ir.Type.Integer(32),
                        lhs.instructions(currentFunction),
                        rhs.instructions(currentFunction)
                    )
                );
                return l;
            }
        }

        record Div(Expression lhs, Expression rhs) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                var l = SSABuilder.localVariable();
                currentFunction.instructions().add(
                    new Instruction.SDiv(
                        l,
                        new dev.akarah.ir.Type.Integer(32),
                        lhs.instructions(currentFunction),
                        rhs.instructions(currentFunction)
                    )
                );
                return l;
            }
        }

        record Negate(Expression value) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                return null;
            }
        }

        record Invoke(Expression base, List<Expression> arguments) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                return null;
            }
        }

        record Subscript(Expression expression, Expression subscriptWith) implements Expression {
            @Override
            public Value.LocalVariable instructions(Function currentFunction) {
                return null;
            }
        }

        Value.LocalVariable instructions(Function currentFunction);
    }
}
