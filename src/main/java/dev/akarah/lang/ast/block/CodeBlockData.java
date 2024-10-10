package dev.akarah.lang.ast.block;

import dev.akarah.lang.ast.Type;
import dev.akarah.llvm.inst.Value;

import java.util.HashMap;

public record CodeBlockData(
    HashMap<String, Type> localVariables,
    HashMap<String, Value.LocalVariable> llvmVariables
) {
}
