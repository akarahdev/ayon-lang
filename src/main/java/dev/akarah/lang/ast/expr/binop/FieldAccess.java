package dev.akarah.lang.ast.expr.binop;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.ProgramTypeInformation;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Constant;
import dev.akarah.llvm.inst.Types;
import dev.akarah.llvm.inst.Value;
import dev.akarah.util.Mutable;

public record FieldAccess(Expression expr, String field, Mutable<Type> type, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        expr.accept(visitor);
        visitor.expression(this);
    }

    @Override
    public Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer) {
        var targetStructureType = ((dev.akarah.lang.ast.Type.UserStructure) this.expr().type().get());
        var targetStructureData = ProgramTypeInformation.resolveStructure(targetStructureType.name(), this.errorSpan());
        var targetFieldType = targetStructureData.parameters().get(this.field());
        var targetFieldIndex = FunctionTransformer.getIndexOf(targetStructureData.parameters(), this.field(), this.errorSpan());
        var ptr = transformer.basicBlocks.peek().getElementPtr(
            ProgramTypeInformation.resolveStructure(((dev.akarah.lang.ast.Type.UserStructure) this.expr().type().get()).name(), this.expr().errorSpan())
                .llvmStruct(),
            transformer.buildExpression(this.expr(), codeBlock, true),
            Types.integer(32),
            Constant.constant(0),
            Types.integer(32),
            Constant.constant(targetFieldIndex + 1)
        );
        if (dereferenceLocals) {
            return transformer.basicBlocks.peek().load(
                targetFieldType.llvm(this.errorSpan()),
                ptr
            );
        } else {
            return ptr;
        }
    }

    @Override
    public String toString() {
        return "(" + expr + "->" + field + " as " + type + ")";
    }
}
