package dev.akarah.lang.ast.expr;

import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.llvm.inst.Value;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

public interface Expression extends AST, Statement {


    Mutable<Type> type();

    void accept(Visitor visitor);

    default void llvm(CodeBlock codeBlock, FunctionTransformer transformer) {
        this.llvm(codeBlock, true, transformer);
    }

    Value llvm(CodeBlock codeBlock, boolean dereferenceLocals, FunctionTransformer transformer);
}
