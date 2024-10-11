package dev.akarah.lang.ast;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.ast.header.Header;
import dev.akarah.lang.ast.stmt.Statement;

public interface AST {
    interface Visitor {
        void header(Header header);
        void beginCodeBlock(CodeBlock codeBlock);
        void endCodeBlock(CodeBlock codeBlock);
        void statement(Statement statement);
        void expression(Expression expression);
    }

    SpanData errorSpan();
}
