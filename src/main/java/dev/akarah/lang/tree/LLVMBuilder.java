package dev.akarah.lang.tree;

import dev.akarah.llvm.Module;
import dev.akarah.llvm.cfg.BasicBlock;
import dev.akarah.llvm.cfg.Function;
import dev.akarah.llvm.inst.Constant;
import dev.akarah.llvm.inst.Instruction;
import dev.akarah.llvm.inst.Types;
import dev.akarah.llvm.inst.Value;

import java.util.List;

public class LLVMBuilder {
    FunctionTypeInformation typeInformation;
    Module module;

    Function function;
    AST.Header.Function astFunction;
    BasicBlock basicBlock;

    public void walkProgram(AST.Program program) {
        module = Module.of("test-module");
        for(var header : program.headers()) {
            switch (header) {
                case AST.Header.Function function -> walkFunction(function);
                case AST.Header.FunctionDeclaration declaration -> declareFunction(declaration);
            }

        }
        module.compile();
    }

    public void declareFunction(AST.Header.FunctionDeclaration function) {
        this.function = Function.of(new Value.GlobalVariable(function.name()));

        for(var param : function.parameters().keySet()) {
            var type = function.parameters().get(param);
            var llvmName = Value.LocalVariable.random();
            this.function.parameter(type.llvm(), llvmName);
        }

        this.function.returns(function.returnType().llvm());

        module.functions.add(this.function);
    }


    public void walkFunction(AST.Header.Function function) {
        this.function = Function.of(new Value.GlobalVariable(function.name()));
        this.astFunction = function;

        for(var param : function.parameters().keySet()) {
            var type = function.parameters().get(param);
            var llvmName = Value.LocalVariable.random();
            this.function.parameter(type.llvm(), llvmName);
            function.codeTypeInformation().v.llvmLocals.put(param, llvmName);
        }


        walkCodeBlock(function.codeBlock());

        this.function.returns(function.returnType().llvm());

        module.functions.add(this.function);
    }

    public void walkCodeBlock(AST.Expression.CodeBlock codeBlock) {
        this.basicBlock = BasicBlock.of(Value.LocalVariable.random());
        var sb = this.basicBlock;

        for(var stmt : codeBlock.statements()) {
            walkStatement(stmt);
        }
        this.function.withBasicBlock(sb);
    }

    public void walkStatement(AST.Statement statement) {
        switch (statement) {
            case AST.Statement.VariableDeclaration variableDeclaration -> {
                astFunction.codeTypeInformation().v.llvmLocals.put(
                    variableDeclaration.name(),
                    basicBlock.alloca(variableDeclaration.type().v.llvm())
                );
            }
            case AST.Statement.ReturnValue returnValue -> {
                System.out.println(returnValue);
                System.out.println(returnValue.value().type());
                if(returnValue.value().type().v.llvm().equals(Types.VOID)) {
                    basicBlock.ret();
                } else {
                    basicBlock.ret(
                        returnValue.value().type().v.llvm(),
                        walkExpression(returnValue.value())
                    );
                }
            }
            case AST.Expression.Conditional conditional -> {
                walkExpression(conditional);
            }
            case AST.Expression expression -> walkExpression(expression);
            default -> throw new RuntimeException("WIP " + statement);
        }
    }

    public Value walkExpression(AST.Expression expression) {
        switch (expression) {
            case AST.Expression.Add add -> {
                return basicBlock.add(
                    add.type().v.llvm(),
                    walkExpression(add.lhs()),
                    walkExpression(add.rhs())
                );
            }
            case AST.Expression.ArrayLiteral arrayLiteral -> {
            }
            case AST.Expression.CodeBlock codeBlock -> {
                walkCodeBlock(codeBlock);
                return null;
            }
            case AST.Expression.Conditional conditional -> {
                var currentBlock = this.basicBlock;
                var condition = walkExpression(conditional.condition());

                var ifTrueBlock = BasicBlock.of(Value.LocalVariable.random());
                var ifFalseBlock = BasicBlock.of(Value.LocalVariable.random());

                currentBlock.br(
                    condition,
                    ifTrueBlock.name(),
                    ifFalseBlock.name()
                );

                var finishingBlock = BasicBlock.of(Value.LocalVariable.random());



                currentBlock.childBlock(ifTrueBlock);
                currentBlock.childBlock(ifFalseBlock);
                currentBlock.childBlock(finishingBlock);

                this.basicBlock = ifTrueBlock;
                switch (conditional.ifTrue()) {
                    case AST.Expression.CodeBlock codeBlock -> {
                        for(var stmt : codeBlock.statements()) {
                            walkStatement(stmt);
                        }
                    }
                    case AST.Expression expr -> this.walkExpression(conditional.ifTrue());
                }

                conditional.ifFalse().ifPresent(d -> {
                    this.basicBlock = ifFalseBlock;
                    switch (d) {
                        case AST.Expression.CodeBlock codeBlock -> {
                            for(var stmt : codeBlock.statements()) {
                                walkStatement(stmt);
                            }
                        }
                        case AST.Expression expr -> this.walkExpression(conditional.ifTrue());
                    }
                });

                ifTrueBlock.br(finishingBlock.name());
                ifFalseBlock.br(finishingBlock.name());

                this.basicBlock = finishingBlock;

                return finishingBlock.name();
            }
            case AST.Expression.Div div -> {
                return basicBlock.sdiv(
                    div.type().v.llvm(),
                    walkExpression(div.lhs()),
                    walkExpression(div.rhs())
                );
            }
            case AST.Expression.FloatingLiteral floatingLiteral -> {
                return Constant.constant(floatingLiteral.floating());
            }
            case AST.Expression.IntegerLiteral integerLiteral -> {
                return Constant.constant(integerLiteral.integer());
            }
            case AST.Expression.Invoke invoke -> {
                switch (invoke.base()) {
                    case AST.Expression.VariableLiteral variableLiteral -> {
                        var newParams = invoke.arguments().stream()
                            .map(it -> new Instruction.Call.Parameter(
                                it.type().v.llvm(),
                                walkExpression(it)
                            ))
                            .toList();
                        return this.basicBlock.call(
                            ProgramTypeInformation.functions.get(variableLiteral.name()).returnType().llvm(),
                            new Value.GlobalVariable(variableLiteral.name()),
                            newParams
                        );
                    }
                    default -> {}
                }
            }
            case AST.Expression.Mul mul -> {
                return basicBlock.mul(
                    mul.type().v.llvm(),
                    walkExpression(mul.lhs()),
                    walkExpression(mul.rhs())
                );
            }
            case AST.Expression.Negate negate -> {
            }
            case AST.Expression.Sub sub -> {
                return basicBlock.sub(
                    sub.type().v.llvm(),
                    walkExpression(sub.lhs()),
                    walkExpression(sub.rhs())
                );
            }
            case AST.Expression.Subscript subscript -> {
            }
            case AST.Expression.VariableLiteral variableLiteral -> {
                return basicBlock.load(
                    astFunction.codeTypeInformation().v.locals.get(variableLiteral.name()).llvm(),
                    astFunction.codeTypeInformation().v.llvmLocals.get(variableLiteral.name())
                );
            }
        }
        throw new UnsupportedOperationException("WIP " + expression);
    }
}
