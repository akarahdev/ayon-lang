package dev.akarah;

import dev.akarah.lang.ast.Program;
import dev.akarah.lang.ast.header.Function;
import dev.akarah.lang.ast.header.FunctionDeclaration;
import dev.akarah.lang.lexer.Lexer;
import dev.akarah.lang.lexer.StringReader;
import dev.akarah.lang.parser.Parser;
import dev.akarah.lang.parser.TokenReader;
import dev.akarah.lang.ast.FunctionTypeAnnotator;
import dev.akarah.lang.ast.ProgramTypeInformation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException {

        final Program[] program = {new Program(new ArrayList<>())};
        Files.walk(Path.of("./src/")).forEach(file -> {
            try {
                var contents = Files.readString(file);
                var tokens = new Lexer(new StringReader(contents, file.getFileName().toString())).lex();
                var parser = new Parser(new TokenReader(tokens));
                program[0] = program[0].join(parser.parseAll());
            } catch (IOException e) {

            }
        });

        System.out.println(program[0]);

        for(var header : program[0].headers()) {
            switch (header) {
                case Function function -> {
                    ProgramTypeInformation.functions.put(function.name(), new FunctionDeclaration(
                        function.name(),
                        function.parameters(),
                        function.returnType()
                    ));
                }
                case FunctionDeclaration functionDeclaration -> {
                    ProgramTypeInformation.functions.put(functionDeclaration.name(), functionDeclaration);
                }
                default -> throw new IllegalStateException("Unexpected value: " + header);
            }
        }

        for(var header : program[0].headers()) {
            switch (header) {
                case Function function -> {
                    var ftd = new FunctionTypeAnnotator();
                    ftd.header(function);

                    function.visit(ftd);

                    System.out.println(function);
                }
                case FunctionDeclaration functionDeclaration -> {
                    System.out.println(functionDeclaration);
                }
                default -> throw new IllegalStateException("Unexpected value: " + header);
            }
        }
    }
}