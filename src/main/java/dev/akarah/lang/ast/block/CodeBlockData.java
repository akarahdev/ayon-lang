package dev.akarah.lang.ast.block;

import dev.akarah.lang.ast.Type;

import java.util.HashMap;

public record CodeBlockData(
    HashMap<String, Type> localVariables
) {
}
