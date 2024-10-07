package dev.akarah;

import dev.akarah.lang.lexer.Lexer;
import dev.akarah.lang.lexer.StringReader;
import dev.akarah.lang.parser.Parser;
import dev.akarah.lang.parser.TokenReader;
import dev.akarah.lang.tree.AST;
import dev.akarah.lang.tree.FunctionTypeInformation;
import dev.akarah.lang.tree.ProgramTypeInformation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException {

        final AST.Program[] program = {new AST.Program(new ArrayList<>())};
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
                case AST.Header.Function function -> {
                    ProgramTypeInformation.functions.put(function.name(), new AST.Header.FunctionDeclaration(
                        function.name(),
                        function.parameters(),
                        function.returnType()
                    ));
                }
                case AST.Header.FunctionDeclaration functionDeclaration -> {
                    ProgramTypeInformation.functions.put(functionDeclaration.name(), functionDeclaration);
                }
            }
        }

        for(var header : program[0].headers()) {
            switch (header) {
                case AST.Header.Function function -> {
                    var ftd = new FunctionTypeInformation();
                    ftd.header(function);

                    function.visit(ftd);
                    function.codeTypeInformation().v = ftd;

                    System.out.println(function);
                }
                case AST.Header.FunctionDeclaration functionDeclaration -> {
                    System.out.println(functionDeclaration);
                }
            }
        }
    }
}