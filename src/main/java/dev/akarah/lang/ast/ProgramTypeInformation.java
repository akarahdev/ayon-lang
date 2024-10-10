package dev.akarah.lang.ast;

import dev.akarah.lang.ast.header.Function;
import dev.akarah.lang.ast.header.FunctionDeclaration;
import dev.akarah.lang.ast.header.Header;
import dev.akarah.lang.ast.header.StructureDeclaration;

import java.util.HashMap;

public class ProgramTypeInformation {
    public static HashMap<String, Header> headers = new HashMap<>();

    public static void registerFunction(Function function) {
        headers.put(
            function.name(),
            function
        );
    }

    public static void registerFunctionDeclaration(FunctionDeclaration function) {
        headers.put(
            function.name(),
            function
        );
    }

    public static void registerStructure(StructureDeclaration structureDeclaration) {
        headers.put(
            structureDeclaration.name(),
            structureDeclaration
        );
    }


    public static Header resolveHeader(String name) {
        return headers.get(name);
    }

    public static FunctionDeclaration resolveFunction(String name) {
        System.out.println(name);
        var tmp = headers.get(name);
        return switch (tmp) {
            case Function function -> new FunctionDeclaration(
                function.name(),
                function.parameters(),
                function.returnType(),
                function.attributes()
            );
            case FunctionDeclaration declaration -> declaration;
            default -> throw new IllegalStateException("Unexpected value: " + tmp);
        };
    }

    public static StructureDeclaration resolveStructure(String name) {
        var tmp = headers.get(name);
        assert tmp instanceof StructureDeclaration;
        return (StructureDeclaration) tmp;
    }
}
