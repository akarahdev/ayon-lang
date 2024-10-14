package dev.akarah.lang.ast.expr.literal;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.util.Mutable;

import java.nio.charset.StandardCharsets;

public record CStringLiteral(String contents, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        var global = Value.GlobalVariable.random();
        transformer.module.newGlobal(
            global,
            globalVariable -> {
                globalVariable.withType(new dev.akarah.llvm.inst.Type.Array(
                    this.contents().getBytes(StandardCharsets.UTF_8).length + 1,
                    new dev.akarah.llvm.inst.Type.Integer(8)
                ));
                globalVariable.withValue(new Value.CStringConstant(this.contents() + "\\\\00"));
            }
        );
        return global;
    }

    @Override
    public Mutable<Type> type() {
        return new Mutable<>(
            new Type.CStringPointer(new Type.CArray(new Type.Integer(8), this.contents().getBytes(StandardCharsets.UTF_8).length + 1))
        );
    }

    @Override
    public String toString() {
        return ("cstr(" + contents.toString()
            .replace("\\00", "")
            .replace("\"", "") + ')')
            ;
    }
}
