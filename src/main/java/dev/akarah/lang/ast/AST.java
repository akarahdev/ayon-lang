package dev.akarah.lang.ast;

import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.lang.ast.header.Header;
import dev.akarah.lang.ast.stmt.Statement;

public interface AST {
    interface Visitor {
        void header(Header header);

        void statement(Statement statement);

        void expression(Expression expression);
    }


}
