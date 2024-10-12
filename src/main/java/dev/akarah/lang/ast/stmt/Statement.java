package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.llvm.FunctionTransformer;

public interface Statement extends AST {
    void accept(Visitor visitor);

    void llvm(CodeBlock codeBlock, FunctionTransformer transformer);
}
