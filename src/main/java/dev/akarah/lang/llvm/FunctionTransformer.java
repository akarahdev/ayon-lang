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

    public Stack<BasicBlock> basicBlocks = new Stack<>();

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
        statement.llvm(codeBlock, this);
        basicBlocks.peek().comment("end statement: " + statement);
    }

    public Value buildExpression(Expression expression, CodeBlock codeBlock, boolean dereferenceLocals) {
        basicBlocks.peek().comment("begin expression: " + expression);
        var e = expression.llvm(codeBlock, dereferenceLocals, this);
        basicBlocks.peek().comment("end expression: " + expression);
        return e;
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


