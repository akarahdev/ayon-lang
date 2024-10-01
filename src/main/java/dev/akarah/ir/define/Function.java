package dev.akarah.ir.define;

import dev.akarah.ir.Type;
import dev.akarah.ir.Value;
import dev.akarah.ir.inst.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record Function(
    Value.GlobalVariable name,
    Type returnType,
    Map<Value.LocalVariable, Type> parameters,
    List<Instruction> instructions,
    Map<String, Value.LocalVariable> locals
) {
    public static Function of(Value.GlobalVariable name, Type returnType) {
        return new Function(
            name,
            returnType,
            new HashMap<>(),
            new ArrayList<>(),
            new HashMap<>()
        );
    }

    public Function parameter(Type type, Value.LocalVariable variable) {
        this.parameters.put(variable, type);
        return this;
    }

    @Override
    public String toString() {
        return "define " + this.returnType + " " + this.name + "() {\n" +
            this.instructions
                .stream()
                .map(Object::toString)
                .map(it -> {
                    if(it.endsWith(":"))
                        return "  " + it;
                    else
                        return "    " + it;
                })
                .collect(Collectors.joining("\n"))
            + "\n}";
    }
}
