package dev.akarah.lang.llvm;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.ProgramTypeInformation;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.*;
import dev.akarah.lang.ast.stmt.*;
import dev.akarah.lang.error.CompileError;
import dev.akarah.llvm.Module;
import dev.akarah.llvm.cfg.BasicBlock;
import dev.akarah.llvm.cfg.Function;
import dev.akarah.llvm.inst.Constant;
import dev.akarah.llvm.inst.Instruction;
import dev.akarah.llvm.inst.Type;
import dev.akarah.llvm.inst.Value;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

public class FunctionTransformer {
    public Module module;
    public Function ownedFunction;

    public static String mangle(String name) {
        var function = ProgramTypeInformation.resolveFunction(name);
        if (function != null) {
            AtomicReference<String> output = new AtomicReference<>(name.replace("::", "."));
            function.attributes().stream()
                .filter(it -> it.name().equals("mangle_as"))
                .findFirst()
                .ifPresent(attribute -> {
                    output.set(((CStringLiteral) attribute.arguments().getFirst()).contents());
                });
            return output.get();
        }
        throw new CompileError.RawMessage("unable to mangle " + name, new SpanData("main.ayon", 0, 0));
    }

    Stack<BasicBlock> basicBlocks = new Stack<>();

    public Function transform(dev.akarah.lang.ast.header.Function function, Function f) {
        var bb = BasicBlock.of(new Value.LocalVariable("setup"));
        for (var parameter : function.parameters().keySet()) {
            var p = Value.LocalVariable.random();
            f.parameter(
                function.parameters().get(parameter).llvm(),
                p
            );
            var pPtr = bb.alloca(function.parameters().get(parameter).llvm());
            bb.store(function.parameters().get(parameter).llvm(), p, pPtr);
            function.codeBlock().data().llvmVariables().put(parameter, (Value.LocalVariable) pPtr);
        }
        var ch = convertCodeBlock(function.codeBlock());
        bb.br(ch.name());
        bb.childBlock(ch);
        f.returns(function.returnType().llvm());
        f.withBasicBlock(bb);
        return f;
    }

    public Function transform(dev.akarah.lang.ast.header.FunctionDeclaration function, Function f) {
        for (var parameter : function.parameters().keySet()) {
            f.parameter(
                function.parameters().get(parameter).llvm(),
                new Value.LocalVariable(parameter)
            );
        }
        f.returns(function.returnType().llvm());
        return f;
    }

    public BasicBlock convertCodeBlock(CodeBlock codeBlock) {
        var basicBlock = BasicBlock.of(Value.LocalVariable.random());
        basicBlocks.add(basicBlock);
        for (var stmt : codeBlock.statements()) {
            buildStatement(stmt, codeBlock);
        }
        return basicBlock;
    }

    public void buildStatement(Statement statement, CodeBlock codeBlock) {
        switch (statement) {
            case VariableDeclaration variableDeclaration -> {
                var local = (Value.LocalVariable) basicBlocks.peek().alloca(variableDeclaration.type().get().llvm());
                codeBlock.data().llvmVariables().put(variableDeclaration.name(), local);
                var expr = buildExpression(variableDeclaration.value(), codeBlock);
                basicBlocks.peek().store(variableDeclaration.type().get().llvm(), expr, local);
            }
            case IfStatement ifStatement -> {

            }
            case WhileLoop whileLoop -> {

            }
            case ReturnValue returnValue -> {
                basicBlocks.peek().ret(
                    returnValue.value().type().get().llvm(),
                    buildExpression(returnValue.value(), codeBlock)
                );
            }
            case Expression expression -> {
                buildExpression(expression, codeBlock);
            }
            default -> throw new IllegalStateException("Unexpected value: " + statement);
        }
    }

    public Value buildExpression(Expression expression, CodeBlock codeBlock) {
        return switch (expression) {
            case IntegerLiteral integerLiteral -> Constant.constant(integerLiteral.integer());
            case FloatingLiteral floatingLiteral -> Constant.constant(floatingLiteral.floating());
            case VariableLiteral variableLiteral -> {
                System.out.println(codeBlock.data().llvmVariables().keySet());
                yield basicBlocks.peek().load(
                    variableLiteral.type().get().llvm(),
                    codeBlock.data().llvmVariables().get(variableLiteral.name())
                );
            }
            case CStringLiteral cStringLiteral -> {
                var global = Value.GlobalVariable.random();
                module.newGlobal(
                    global,
                    globalVariable -> {
                        globalVariable.withType(new Type.Array(
                            cStringLiteral.contents().getBytes(StandardCharsets.UTF_8).length + 1,
                            new Type.Integer(8)
                        ));
                        globalVariable.withValue(new Value.CStringConstant(cStringLiteral.contents() + "\\\\00"));
                    }
                );
                yield global;
            }
            case Invoke invoke -> {
                if (invoke.base() instanceof VariableLiteral variableLiteral) {
                    var arguments = new ArrayList<Instruction.Call.Parameter>();

                    for (var value : invoke.arguments()) {
                        arguments.add(new Instruction.Call.Parameter(
                            value.type().get().llvm(),
                            buildExpression(value, codeBlock)
                        ));
                    }
                    yield basicBlocks.peek().call(
                        invoke.type().get().llvm(),
                        new Value.GlobalVariable(FunctionTransformer.mangle(variableLiteral.name())),
                        arguments
                    );
                }
                throw new IllegalStateException("Unexpected value: " + expression);
            }
            case Add binOp -> {
                yield basicBlocks.peek().add(binOp.type().get().llvm(),
                    buildExpression(binOp.lhs(), codeBlock),
                    buildExpression(binOp.rhs(), codeBlock));
            }
            case Sub binOp -> {
                yield basicBlocks.peek().sub(binOp.type().get().llvm(),
                    buildExpression(binOp.lhs(), codeBlock),
                    buildExpression(binOp.rhs(), codeBlock));
            }
            case Mul binOp -> {
                yield basicBlocks.peek().mul(binOp.type().get().llvm(),
                    buildExpression(binOp.lhs(), codeBlock),
                    buildExpression(binOp.rhs(), codeBlock));
            }
            case Div binOp -> {
                yield basicBlocks.peek().sdiv(binOp.type().get().llvm(),
                    buildExpression(binOp.lhs(), codeBlock),
                    buildExpression(binOp.rhs(), codeBlock));
            }
            case BitCast bitCast -> {
                throw new RuntimeException("TODO");
            }
            case InitStructure initStructure -> {
//                yield basicBlocks.peek().call(
//                    new Type.Ptr(Types.VOID),
//                    new Value.GlobalVariable("malloc"),
//                    List.of(new Instruction.Call.Parameter(new Type.Integer(32), Constant.constant(initStructure.type().get().size()+4)))
//                );
//                yield basicBlocks.peek().alloca(initStructure.type().get().llvm());
                yield null;
            }
            case FieldAccess access -> {
                yield null;
            }
            case Store store -> switch (store.lhs()) {
//                case VariableLiteral variableLiteral -> {
//                    basicBlocks.peek().store(
//                        variableLiteral.type().get().llvm(),
//                        buildExpression(store.rhs(), codeBlock),
//                        codeBlock.data().llvmVariables().get(variableLiteral.name())
//                    );
//                    yield null;
//                }
                default -> null;
            };
            default -> throw new IllegalStateException("Unexpected value: " + expression);
        };
    }
}


