package dev.akarah.lang.ast.stmt;

import dev.akarah.lang.ast.AST;

public interface Statement extends AST {
    void accept(Visitor visitor);
}
