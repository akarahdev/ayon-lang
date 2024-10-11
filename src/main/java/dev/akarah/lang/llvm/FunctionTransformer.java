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
import dev.akarah.llvm.inst.*;
import dev.akarah.util.ReferenceCountingLibrary;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        basicBlocks.peek().comment("begin statement: " + statement);
        switch (statement) {
            case VariableDeclaration variableDeclaration -> {
                var local = (Value.LocalVariable) basicBlocks.peek().alloca(variableDeclaration.type().get().llvm());
                codeBlock.data().llvmVariables().put(variableDeclaration.name(), local);
                var expr = buildExpression(variableDeclaration.value(), codeBlock, true);
                basicBlocks.peek().store(variableDeclaration.type().get().llvm(), expr, local);
            }
            case IfStatement ifStatement -> {

            }
            case WhileLoop whileLoop -> {

            }
            case ReturnValue returnValue -> {
                var e = buildExpression(returnValue.value(), codeBlock, true);
                for(var variableName : codeBlock.data().localVariables().keySet()) {
                    if(codeBlock.data().localVariables().get(variableName) instanceof dev.akarah.lang.ast.Type.UserStructure) {
                        basicBlocks.peek().call(
                            Types.integer(16),
                            ReferenceCountingLibrary.DECREMENT_REFERENCE_COUNT,
                            List.of(new Instruction.Call.Parameter(
                                Types.pointerTo(Types.VOID),
                                codeBlock.data().llvmVariables().get(variableName)
                            ))
                        );
                    }
                }
                basicBlocks.peek().ret(
                    returnValue.value().type().get().llvm(),
                    e
                );
            }
            case Expression expression -> {
                buildExpression(expression, codeBlock, true);
            }
            default -> throw new IllegalStateException("Unexpected value: " + statement);
        }
        basicBlocks.peek().comment("end statement: " + statement);
    }

    public Value buildExpression(Expression expression, CodeBlock codeBlock, boolean dereferenceLocals) {
        return switch (expression) {
            case IntegerLiteral integerLiteral -> Constant.constant(integerLiteral.integer());
            case FloatingLiteral floatingLiteral -> Constant.constant(floatingLiteral.floating());
            case VariableLiteral variableLiteral -> {
                System.out.println(codeBlock.data().llvmVariables().keySet());
                if(dereferenceLocals) {
                    yield basicBlocks.peek().load(
                        variableLiteral.type().get().llvm(),
                        codeBlock.data().llvmVariables().get(variableLiteral.name())
                    );
                } else {
                    yield codeBlock.data().llvmVariables().get(variableLiteral.name());
                }
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
                        System.out.println(value);
                        arguments.add(new Instruction.Call.Parameter(
                            value.type().get().llvm(),
                            buildExpression(value, codeBlock, true)
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
                    buildExpression(binOp.lhs(), codeBlock, true),
                    buildExpression(binOp.rhs(), codeBlock, true));
            }
            case Sub binOp -> {
                yield basicBlocks.peek().sub(binOp.type().get().llvm(),
                    buildExpression(binOp.lhs(), codeBlock, true),
                    buildExpression(binOp.rhs(), codeBlock, true));
            }
            case Mul binOp -> {
                yield basicBlocks.peek().mul(binOp.type().get().llvm(),
                    buildExpression(binOp.lhs(), codeBlock, true),
                    buildExpression(binOp.rhs(), codeBlock, true));
            }
            case Div binOp -> {
                yield basicBlocks.peek().sdiv(binOp.type().get().llvm(),
                    buildExpression(binOp.lhs(), codeBlock, true),
                    buildExpression(binOp.rhs(), codeBlock, true));
            }
            case BitCast bitCast -> {
                yield basicBlocks.peek().bitcast(
                    bitCast.expr().type().get().llvm(),
                    buildExpression(bitCast.expr(), codeBlock, true),
                    bitCast.type().get().llvm()
                );
            }
            case InitStructure initStructure -> {
                var ptr = basicBlocks.peek().call(
                    Types.pointerTo(Types.VOID),
                    new Value.GlobalVariable("malloc"),
                    List.of(
                        new Instruction.Call.Parameter(
                            Types.integer(32), Constant.constant(initStructure.type().get().size())))
                );
                basicBlocks.peek().store(
                    initStructure.type().get().llvm(),
                    new Value.ZeroInitializer(),
                    ptr
                );
                basicBlocks.peek().call(
                    Types.integer(16),
                    ReferenceCountingLibrary.INCREMENT_REFERENCE_COUNT,
                    List.of(new Instruction.Call.Parameter(Types.pointerTo(Types.VOID), ptr))
                );
                yield ptr;
            }
            case FieldAccess access -> {
                var targetStructureType = ((dev.akarah.lang.ast.Type.UserStructure) access.expr().type().get());
                var targetStructureData = ProgramTypeInformation.resolveStructure(targetStructureType.name());
                var targetFieldType = targetStructureData.parameters().get(access.field());
                var targetFieldIndex = getIndexOf(targetStructureData.parameters(), access.field());
                var ptr = basicBlocks.peek().getElementPtr(
                    ((Type.Ptr) access.expr().type().get().llvm()).subtype(),
                    buildExpression(access.expr(), codeBlock, false),
                    Types.integer(32),
                    Constant.constant(0),
                    Types.integer(32),
                    Constant.constant(targetFieldIndex)
                );
                if(dereferenceLocals) {
                    yield basicBlocks.peek().load(
                       targetFieldType.llvm(),
                        ptr
                    );
                } else {
                    yield ptr;
                }
            }
            case Store store -> {
                var ref = buildExpression(store.lhs(), codeBlock, false);
                basicBlocks.peek().store(
                    store.lhs().type().get().llvm(),
                    buildExpression(store.rhs(), codeBlock, true),
                    ref
                );
                yield null;
            }
            default -> throw new IllegalStateException("Unexpected value: " + expression);
        };

    }

    public static<K, V> int getIndexOf(LinkedHashMap<K, V> map, K key) {
        int index = 0;
        for(var key2 : map.keySet()) {
            if(key.equals(key2))
                return index;
            index++;
        }
        throw new RuntimeException("ruh roh key not found!! oopsie doopsie :3");
    }
}


