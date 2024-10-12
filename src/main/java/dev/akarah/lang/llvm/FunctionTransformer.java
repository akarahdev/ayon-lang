package dev.akarah.lang.llvm;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.ProgramTypeInformation;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.*;
import dev.akarah.lang.ast.expr.binop.*;
import dev.akarah.lang.ast.expr.cmp.*;
import dev.akarah.lang.ast.expr.literal.*;
import dev.akarah.lang.ast.expr.unop.BitCast;
import dev.akarah.lang.ast.header.FunctionDeclaration;
import dev.akarah.lang.ast.stmt.*;
import dev.akarah.lang.error.CompileError;
import dev.akarah.llvm.Module;
import dev.akarah.llvm.cfg.BasicBlock;
import dev.akarah.llvm.cfg.Function;
import dev.akarah.llvm.inst.*;
import dev.akarah.llvm.inst.memory.Alloca;
import dev.akarah.llvm.inst.misc.Call;
import dev.akarah.llvm.inst.ops.ComparisonOperation;
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

    public static String mangle(String name, SpanData span) {
        var function = ProgramTypeInformation.resolveFunction(name, span);
        if (function != null) {
            AtomicReference<String> output = new AtomicReference<>(name.replace("::", "."));
            function.attributes().stream()
                .filter(it -> it.name().equals("mangle_as"))
                .findFirst()
                .ifPresent(attribute -> {
                    output.set(((StdStringLiteral) attribute.arguments().getFirst()).contents());
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
                function.parameters().get(parameter).llvm(function.errorSpan()),
                p
            );
            var pPtr = bb.alloca(function.parameters().get(parameter).llvm(function.errorSpan()));
            bb.store(function.parameters().get(parameter).llvm(function.errorSpan()), p, pPtr);
            function.codeBlock().data().llvmVariables().put(parameter, (Value.LocalVariable) pPtr);
        }
        var ch = convertCodeBlock(function.codeBlock());
        bb.br(ch.name());
        bb.childBlock(ch);
        f.returns(function.returnType().llvm(function.errorSpan()));
        f.withBasicBlock(bb);
        return f;
    }

    public Function transform(FunctionDeclaration function, Function f) {
        for (var parameter : function.parameters().keySet()) {
            f.parameter(
                function.parameters().get(parameter).llvm(function.errorSpan()),
                new Value.LocalVariable(parameter)
            );
        }
        f.returns(function.returnType().llvm(function.errorSpan()));
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
                var local = new Value.LocalVariable(
                    Value.LocalVariable.random().name()
                        + ".local."
                        + variableDeclaration.name().replace("::", "."));
                basicBlocks.peek().perform(new Alloca(
                    local,
                    variableDeclaration.type().get().llvm(variableDeclaration.errorSpan()),
                    1
                ));
                codeBlock.data().llvmVariables().put(variableDeclaration.name(), local);
                var expr = buildExpression(variableDeclaration.value(), codeBlock, true);
                basicBlocks.peek().store(variableDeclaration.type().get().llvm(variableDeclaration.errorSpan()), expr, local);
            }
            case IfStatement ifStatement -> {
                var condition = buildExpression(ifStatement.condition(), codeBlock, true);
                var finishingBlock = BasicBlock.of(Value.LocalVariable.random());
                basicBlocks.peek().ifThenElse(
                    condition,
                    trueBlock -> {
                        basicBlocks.add(trueBlock);
                        for (var stmt : ifStatement.ifTrue().statements()) {
                            buildStatement(stmt, codeBlock);
                        }

                        if(ifStatement.ifTrue().statements().stream().noneMatch(it -> it instanceof ReturnValue)) {
                            basicBlocks.peek().br(finishingBlock.name());
                        }

                        basicBlocks.remove(trueBlock);
                    },
                    falseBlock -> {
                        basicBlocks.add(falseBlock);
                        for (var stmt : ifStatement.ifFalse().statements()) {
                            buildStatement(stmt, codeBlock);
                        }


                        if(ifStatement.ifFalse().statements().stream().noneMatch(it -> it instanceof ReturnValue)) {
                            basicBlocks.peek().br(finishingBlock.name());
                        }

                        basicBlocks.remove(falseBlock);
                    }
                );
                basicBlocks.peek().childBlock(finishingBlock);
                basicBlocks.add(finishingBlock);

            }
            case WhileLoop whileLoop -> {

            }
            case ReturnValue returnValue -> {
                Value e = null;
                if (returnValue.value().type().get() instanceof dev.akarah.lang.ast.Type.UserStructure userStructure) {
                    e = buildExpression(returnValue.value(), codeBlock, true);
                } else {
                    e = buildExpression(returnValue.value(), codeBlock, true);
                }
                for (var variableName : codeBlock.data().localVariables().keySet()) {
                    if (codeBlock.data().localVariables().get(variableName) instanceof dev.akarah.lang.ast.Type.UserStructure) {
                        basicBlocks.peek().call(
                            Types.integer(16),
                            ReferenceCountingLibrary.DECREMENT_REFERENCE_COUNT,
                            List.of(new Call.Parameter(
                                Types.pointerTo(Types.VOID),
                                codeBlock.data().llvmVariables().get(variableName)
                            ))
                        );
                    }
                }
                basicBlocks.peek().ret(
                    returnValue.value().type().get().llvm(returnValue.errorSpan()),
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
                if (dereferenceLocals) {
                    yield basicBlocks.peek().load(
                        variableLiteral.type().get().llvm(expression.errorSpan()),
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
            case StdStringLiteral stdStringLiteral -> {
                var global = Value.GlobalVariable.random();
                module.newGlobal(
                    global,
                    globalVariable -> {
                        globalVariable.withType(new Type.Array(
                            stdStringLiteral.contents().getBytes(StandardCharsets.UTF_8).length + 1,
                            new Type.Integer(8)
                        ));
                        globalVariable.withValue(new Value.CStringConstant(stdStringLiteral.contents() + "\\\\00"));
                    }
                );

                yield basicBlocks.peek().call(
                    Types.pointer(),
                    new Value.GlobalVariable("std.string.of"),
                    List.of(new Call.Parameter(Types.pointer(), global))
                );
            }
            case Invoke invoke -> {
                if (invoke.base() instanceof VariableLiteral variableLiteral) {
                    var arguments = new ArrayList<Call.Parameter>();

                    for (var value : invoke.arguments()) {
                        Value newArg = null;
                        newArg = buildExpression(value, codeBlock, true);
                        arguments.add(new Call.Parameter(
                            value.type().get().llvm(expression.errorSpan()),
                            newArg
                        ));
                    }
                    yield basicBlocks.peek().call(
                        invoke.type().get().llvm(expression.errorSpan()),
                        new Value.GlobalVariable(FunctionTransformer.mangle(variableLiteral.name(), variableLiteral.errorSpan())),
                        arguments
                    );
                }
                throw new IllegalStateException("Unexpected value: " + expression);
            }
            case Add binOp -> {
                var t = binOp.lhs().type().get();
                if (t.isInteger()) {
                    yield basicBlocks.peek().add(binOp.type().get().llvm(expression.errorSpan()),
                        buildExpression(binOp.lhs(), codeBlock, true),
                        buildExpression(binOp.rhs(), codeBlock, true));
                } else if (t.isFloat()) {
                    yield basicBlocks.peek().add(binOp.type().get().llvm(expression.errorSpan()),
                        buildExpression(binOp.lhs(), codeBlock, true),
                        buildExpression(binOp.rhs(), codeBlock, true));
                } else if (t.isRecord()) {
                    var functionName = ((dev.akarah.lang.ast.Type.UserStructure) t).name() + "::add";
                    ProgramTypeInformation.resolveFunction(functionName, binOp.errorSpan());
                    yield basicBlocks.peek().call(
                        t.llvm(binOp.errorSpan()),
                        new Value.GlobalVariable(mangle(functionName, binOp.errorSpan())),
                        List.of(
                            new Call.Parameter(t.llvm(binOp.errorSpan()), buildExpression(binOp.lhs(), codeBlock, true)),
                            new Call.Parameter(t.llvm(binOp.errorSpan()), buildExpression(binOp.rhs(), codeBlock, true))
                        )
                    );
                } else throw new CompileError.RawMessage("'+' is not supported on these operands", binOp.errorSpan());
            }
            case Sub binOp -> {
                var t = binOp.lhs().type().get();
                if (t.isInteger()) {
                    yield basicBlocks.peek().add(binOp.type().get().llvm(expression.errorSpan()),
                        buildExpression(binOp.lhs(), codeBlock, true),
                        buildExpression(binOp.rhs(), codeBlock, true));
                } else if (t.isFloat()) {
                    yield basicBlocks.peek().sub(binOp.type().get().llvm(expression.errorSpan()),
                        buildExpression(binOp.lhs(), codeBlock, true),
                        buildExpression(binOp.rhs(), codeBlock, true));
                } else if (t.isRecord()) {
                    var functionName = ((dev.akarah.lang.ast.Type.UserStructure) t).name() + "::sub";
                    ProgramTypeInformation.resolveFunction(functionName, binOp.errorSpan());
                    yield basicBlocks.peek().call(
                        t.llvm(binOp.errorSpan()),
                        new Value.GlobalVariable(mangle(functionName, binOp.errorSpan())),
                        List.of(
                            new Call.Parameter(t.llvm(binOp.errorSpan()), buildExpression(binOp.lhs(), codeBlock, true)),
                            new Call.Parameter(t.llvm(binOp.errorSpan()), buildExpression(binOp.rhs(), codeBlock, true))
                        )
                    );
                } else throw new CompileError.RawMessage("'-' is not supported on these operands", binOp.errorSpan());
            }
            case Mul binOp -> {
                var t = binOp.lhs().type().get();
                if (t.isInteger()) {
                    yield basicBlocks.peek().mul(binOp.type().get().llvm(expression.errorSpan()),
                        buildExpression(binOp.lhs(), codeBlock, true),
                        buildExpression(binOp.rhs(), codeBlock, true));
                } else if (t.isFloat()) {
                    yield basicBlocks.peek().mul(binOp.type().get().llvm(expression.errorSpan()),
                        buildExpression(binOp.lhs(), codeBlock, true),
                        buildExpression(binOp.rhs(), codeBlock, true));
                } else if (t.isRecord()) {
                    var functionName = ((dev.akarah.lang.ast.Type.UserStructure) t).name() + "::mul";
                    ProgramTypeInformation.resolveFunction(functionName, binOp.errorSpan());
                    yield basicBlocks.peek().call(
                        t.llvm(binOp.errorSpan()),
                        new Value.GlobalVariable(mangle(functionName, binOp.errorSpan())),
                        List.of(
                            new Call.Parameter(t.llvm(binOp.errorSpan()), buildExpression(binOp.lhs(), codeBlock, true)),
                            new Call.Parameter(t.llvm(binOp.errorSpan()), buildExpression(binOp.rhs(), codeBlock, true))
                        )
                    );
                } else throw new CompileError.RawMessage("'*' is not supported on these operands", binOp.errorSpan());
            }
            case Div binOp -> {
                var t = binOp.lhs().type().get();
                if (t.isInteger()) {
                    yield basicBlocks.peek().add(binOp.type().get().llvm(expression.errorSpan()),
                        buildExpression(binOp.lhs(), codeBlock, true),
                        buildExpression(binOp.rhs(), codeBlock, true));
                } else if (t.isFloat()) {
                    yield basicBlocks.peek().add(binOp.type().get().llvm(expression.errorSpan()),
                        buildExpression(binOp.lhs(), codeBlock, true),
                        buildExpression(binOp.rhs(), codeBlock, true));
                } else if (t.isRecord()) {
                    var functionName = ((dev.akarah.lang.ast.Type.UserStructure) t).name() + "::div";
                    ProgramTypeInformation.resolveFunction(functionName, binOp.errorSpan());
                    yield basicBlocks.peek().call(
                        t.llvm(binOp.errorSpan()),
                        new Value.GlobalVariable(mangle(functionName, binOp.errorSpan())),
                        List.of(
                            new Call.Parameter(t.llvm(binOp.errorSpan()), buildExpression(binOp.lhs(), codeBlock, true)),
                            new Call.Parameter(t.llvm(binOp.errorSpan()), buildExpression(binOp.rhs(), codeBlock, true))
                        )
                    );
                } else throw new CompileError.RawMessage("'/' is not supported on these operands", binOp.errorSpan());
            }
            case BitCast bitCast -> {
                yield basicBlocks.peek().bitcast(
                    bitCast.expr().type().get().llvm(expression.errorSpan()),
                    buildExpression(bitCast.expr(), codeBlock, true),
                    bitCast.type().get().llvm(expression.errorSpan())
                );
            }
            case InitStructure initStructure -> {
                var ptr = basicBlocks.peek().call(
                    Types.pointerTo(Types.VOID),
                    new Value.GlobalVariable("malloc"),
                    List.of(
                        new Call.Parameter(
                            Types.integer(32), Constant.constant(initStructure.type().get().size(expression.errorSpan()))))
                );
                basicBlocks.peek().store(
                    initStructure.type().get().llvm(expression.errorSpan()),
                    new Value.ZeroInitializer(),
                    ptr
                );
                basicBlocks.peek().call(
                    Types.integer(16),
                    ReferenceCountingLibrary.INCREMENT_REFERENCE_COUNT,
                    List.of(new Call.Parameter(Types.pointerTo(Types.VOID), ptr))
                );
                yield ptr;
            }
            case FieldAccess access -> {
                var targetStructureType = ((dev.akarah.lang.ast.Type.UserStructure) access.expr().type().get());
                var targetStructureData = ProgramTypeInformation.resolveStructure(targetStructureType.name(), access.errorSpan());
                var targetFieldType = targetStructureData.parameters().get(access.field());
                var targetFieldIndex = getIndexOf(targetStructureData.parameters(), access.field(), access.errorSpan());
                var ptr = basicBlocks.peek().getElementPtr(
                    ProgramTypeInformation.resolveStructure(((dev.akarah.lang.ast.Type.UserStructure) access.expr().type().get()).name(), access.expr().errorSpan())
                        .llvmStruct(),
                    buildExpression(access.expr(), codeBlock, true),
                    Types.integer(32),
                    Constant.constant(0),
                    Types.integer(32),
                    Constant.constant(targetFieldIndex + 1)
                );
                if (dereferenceLocals) {
                    yield basicBlocks.peek().load(
                        targetFieldType.llvm(expression.errorSpan()),
                        ptr
                    );
                } else {
                    yield ptr;
                }
            }
            case Store store -> {
                var ref = buildExpression(store.lhs(), codeBlock, false);
                basicBlocks.peek().store(
                    store.lhs().type().get().llvm(expression.errorSpan()),
                    buildExpression(store.rhs(), codeBlock, true),
                    ref
                );
                yield null;
            }
            case GreaterThan binOp -> basicBlocks.peek().icmp(
                ComparisonOperation.SIGNED_GREATER_THAN,
                binOp.lhs().type().get().llvm(binOp.errorSpan()),
                buildExpression(binOp.lhs(), codeBlock, true),
                buildExpression(binOp.rhs(), codeBlock, true)
            );
            case GreaterThanOrEq binOp -> basicBlocks.peek().icmp(
                ComparisonOperation.SIGNED_GREATER_THAN_OR_EQUAL,
                binOp.lhs().type().get().llvm(binOp.errorSpan()),
                buildExpression(binOp.lhs(), codeBlock, true),
                buildExpression(binOp.rhs(), codeBlock, true)
            );
            case LessThan binOp -> basicBlocks.peek().icmp(
                ComparisonOperation.SIGNED_LESS_THAN,
                binOp.lhs().type().get().llvm(binOp.errorSpan()),
                buildExpression(binOp.lhs(), codeBlock, true),
                buildExpression(binOp.rhs(), codeBlock, true)
            );
            case LessThanOrEq binOp -> basicBlocks.peek().icmp(
                ComparisonOperation.SIGNED_LESS_THAN_OR_EQUAL,
                binOp.lhs().type().get().llvm(binOp.errorSpan()),
                buildExpression(binOp.lhs(), codeBlock, true),
                buildExpression(binOp.rhs(), codeBlock, true)
            );
            case EqualTo binOp -> basicBlocks.peek().icmp(
                ComparisonOperation.EQUAL,
                binOp.lhs().type().get().llvm(binOp.errorSpan()),
                buildExpression(binOp.lhs(), codeBlock, true),
                buildExpression(binOp.rhs(), codeBlock, true)
            );
            case NullLiteral nullptr -> new Value.NullPtr();
            default -> throw new IllegalStateException("Unexpected value: " + expression);
        };

    }

    public static <K, V> int getIndexOf(LinkedHashMap<K, V> map, K key, SpanData spanData) {
        int index = 0;
        for (var key2 : map.keySet()) {
            if (key.equals(key2))
                return index;
            index++;
        }
        throw new CompileError.RawMessage("field " + key + " is not present", spanData);
    }
}


