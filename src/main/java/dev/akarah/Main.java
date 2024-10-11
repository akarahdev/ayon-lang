package dev.akarah;

import dev.akarah.lang.ast.FunctionTypeChecker;
import dev.akarah.lang.ast.Program;
import dev.akarah.lang.ast.header.Function;
import dev.akarah.lang.ast.header.FunctionDeclaration;
import dev.akarah.lang.ast.header.StructureDeclaration;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.lang.error.CompileError;
import dev.akarah.lang.lexer.Lexer;
import dev.akarah.lang.lexer.StringReader;
import dev.akarah.lang.llvm.FunctionTransformer;
import dev.akarah.lang.parser.Parser;
import dev.akarah.lang.parser.TokenReader;
import dev.akarah.lang.ast.FunctionTypeAnnotator;
import dev.akarah.lang.ast.ProgramTypeInformation;
import dev.akarah.llvm.Module;
import dev.akarah.llvm.inst.Value;
import dev.akarah.util.ReferenceCountingLibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Main {
    static final HashMap<String, String> files = new HashMap<>();
    public static void main(String[] args) throws IOException {




        final Program[] program = {new Program(new ArrayList<>())};



        try {
            AtomicBoolean panic = new AtomicBoolean(false);
            Files.walk(Path.of("./src/")).forEach(file -> {
                try {
                    var contents = Files.readString(file);
                    files.put(file.getFileName().toString(), contents);
                    var tokens = new Lexer(new StringReader(contents, file.getFileName().toString())).lex();
                    var parser = new Parser(new TokenReader(tokens));
                    program[0] = program[0].join(parser.parseAll());
                } catch (IOException e) {

                }
            });

            if(panic.get())
                return;

            for(var header : program[0].headers()) {
                switch (header) {
                    case Function function ->
                        ProgramTypeInformation.registerFunction(function);
                    case FunctionDeclaration functionDeclaration ->
                        ProgramTypeInformation.registerFunctionDeclaration(functionDeclaration);
                    case StructureDeclaration structureDeclaration ->
                        ProgramTypeInformation.registerStructure(structureDeclaration);
                    default -> throw new IllegalStateException("Unexpected value: " + header);
                }
            }

            for(var header : program[0].headers()) {
                switch (header) {
                    case StructureDeclaration structureDeclaration -> {

                    }
                    case Function function -> {
                        var ftd = new FunctionTypeAnnotator();
                        ftd.header(function);
                        function.accept(ftd);

                        System.out.println(function.name());
                        System.out.println("-----");
                        System.out.println(
                            function.codeBlock()
                                .statements()
                                .stream()
                                .map(Statement::toString)
                                .collect(Collectors.joining("\n"))
                        );
                        System.out.println("-----");

                        var ftc = new FunctionTypeChecker();
                        function.accept(ftc);
                    }
                    case FunctionDeclaration functionDeclaration -> {

                    }
                    default -> throw new IllegalStateException("Unexpected value: " + header);
                }
            }

            var module = Module.of("test-module");
            module.withLibrary(new ReferenceCountingLibrary());

            for(var header : program[0].headers()) {
                switch (header) {
                    case Function functionS -> {
                        module.newFunction(
                            new Value.GlobalVariable(FunctionTransformer.mangle(functionS.name(), functionS.errorSpan())),
                            function -> {
                                var ftd = new FunctionTransformer();
                                ftd.module = module;
                                ftd.transform(functionS, function);
                            }
                        );
                    }
                    case FunctionDeclaration functionDeclaration -> {
                        module.newFunction(
                            new Value.GlobalVariable(FunctionTransformer.mangle(functionDeclaration.name(), functionDeclaration.errorSpan())),
                            function -> {
                                var ftd = new FunctionTransformer();
                                ftd.module = module;
                                ftd.transform(functionDeclaration, function);
                            }
                        );
                    }
                    default -> {

                    }
                }
            }

            module.compile();
        } catch (CompileError compileError) {
            handleCompileError(compileError);
        }
    }

    public static void handleCompileError(CompileError e) {
        var file = Path.of(e.span().file());
        System.out.println("[Error] in " + file.getFileName().toString() + " at line " + e.span().line());
        System.out.println("| " + e.toErrorMessage());

        var line = files.get(file.getFileName().toString()).split("\n")[e.span().line()-1];
        System.out.println("| " + line);
        System.out.println("| " + " ".repeat(e.span().column()-1) + "^");
        e.printStackTrace();
        System.exit(1);

    }

}