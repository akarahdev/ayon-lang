package dev.akarah.lang.ast;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.header.Function;
import dev.akarah.lang.ast.header.FunctionDeclaration;
import dev.akarah.lang.ast.header.Header;
import dev.akarah.lang.ast.header.StructureDeclaration;
import dev.akarah.lang.error.CompileError;

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

    public static FunctionDeclaration resolveFunction(String name, SpanData span) {
        try {
            var tmp = headers.get(name);
            return switch (tmp) {
                case Function function -> new FunctionDeclaration(
                    function.name(),
                    function.parameters(),
                    function.returnType(),
                    function.attributes(),
                    function.errorSpan()
                );
                case FunctionDeclaration declaration -> declaration;
                default -> throw new CompileError.RawMessage(name + " is not a valid function", span);
            };
        } catch (NullPointerException exception) {
            throw new CompileError.RawMessage("failed to resolve function: '" + name + "'", span);
        }
    }

    public static StructureDeclaration resolveStructure(String name, SpanData span) {
        var tmp = headers.get(name);
        if(!headers.containsKey(name)) {
            throw new CompileError.RawMessage("unable to resolve structure " + name, span);
        }
        if(!(tmp instanceof StructureDeclaration)) {
            throw new CompileError.RawMessage(name + " is not a struct", span);
        }
        return (StructureDeclaration) tmp;
    }
}
