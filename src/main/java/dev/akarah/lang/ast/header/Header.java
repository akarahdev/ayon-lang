package dev.akarah.lang.ast.header;

import dev.akarah.lang.ast.AST;

public interface Header extends AST {
    void visit(Visitor visitor);
}
