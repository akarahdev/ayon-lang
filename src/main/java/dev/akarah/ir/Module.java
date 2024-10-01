package dev.akarah.ir;

import dev.akarah.lang.tree.AST;

import java.util.List;

public record Module(
    List<AST.Header.Function> functions
) {
}
