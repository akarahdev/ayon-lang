package dev.akarah;

import dev.akarah.lang.lexer.Lexer;
import dev.akarah.lang.lexer.StringReader;
import dev.akarah.lang.lexer.Token;
import dev.akarah.lang.parser.Parser;
import dev.akarah.lang.parser.TokenReader;
import dev.akarah.lang.tree.AST;
import dev.akarah.lang.tree.FunctionTypeInformation;
import dev.akarah.lang.tree.LLVMBuilder;
import dev.akarah.lang.tree.ProgramTypeInformation;
import dev.akarah.llvm.Module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        var f = Files.readString(Path.of("./example/hello.ayon"));
        var tok = new Lexer(new StringReader(f, "./example/hello.ayon")).lex();
        System.out.println(
            tok.stream().map(Token::toString).collect(Collectors.joining("\n"))
        );

        System.out.println("----");

        var parser = new Parser(new TokenReader(tok));

        var program = parser.parseAll();

        System.out.println(program.headers().size());

        for(var header : program.headers()) {
            System.out.println("Pre-processing header: " + header);
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

        for(var header : program.headers()) {
            System.out.println("Typechecking header: " + header);
            switch (header) {
                case AST.Header.Function function -> {
                    System.out.println(function);

                    var ftd = new FunctionTypeInformation();
                    ftd.header(function);

                    function.visit(ftd);
                    function.codeTypeInformation().v = ftd;

                    System.out.println("fd: " + function);
                }
                case AST.Header.FunctionDeclaration functionDeclaration -> {}
            }
        }

        new LLVMBuilder().walkProgram(program);
    }
}