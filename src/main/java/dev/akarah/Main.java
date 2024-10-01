package dev.akarah;

import dev.akarah.lang.lexer.Lexer;
import dev.akarah.lang.lexer.StringReader;
import dev.akarah.lang.lexer.Token;
import dev.akarah.lang.parser.Parser;
import dev.akarah.lang.parser.TokenReader;

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
        var tree = parser.parseFunction();
        System.out.println(tree);

        System.out.println(tree.toLLVM());
    }
}