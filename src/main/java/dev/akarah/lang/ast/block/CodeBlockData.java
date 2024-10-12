package dev.akarah.lang.ast.block;

import dev.akarah.lang.ast.Type;
import dev.akarah.llvm.inst.Value;

import java.util.HashMap;
import java.util.List;

public record CodeBlockData(
    HashMap<String, Type> localVariables,
    HashMap<String, Value.LocalVariable> llvmVariables,
    List<Value.LocalVariable> extraAllocations
) {
}
