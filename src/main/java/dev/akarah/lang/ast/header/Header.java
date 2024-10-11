package dev.akarah.lang.ast.header;

import dev.akarah.lang.ast.AST;

import java.util.List;

public interface Header extends AST {
    List<Attribute> attributes();
    void accept(Visitor visitor);
}
