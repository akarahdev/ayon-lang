package dev.akarah.lang.ast.expr;

import dev.akarah.lang.ast.AST;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

public interface Expression extends AST, Statement {

    Mutable<Type> type();

    void accept(Visitor visitor);
}
