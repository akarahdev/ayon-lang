package dev.akarah.lang.lexer;

import dev.akarah.lang.Reader;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    Reader<Character> stringReader;
    List<Token> tokens = new ArrayList<>();

    public static boolean isKeyword(String keyword) {
        return """
            ; Skipped line for other keywords
            fn
            declare
            if
            then
            else
            while
            for
            var
            let
            return
            namespace
            sizeof
            enum
            record
            switch
            match
            default
            box
            new
            true
            false
            bool
            f32
            f64
            f128
            usize
            unit
            as
            break
            goto
            continue
            static
            interface
            async
            await
            abstract
            yield
            override
            final
            macro
            dyn
            """.contains("\n" + keyword + "\n");
    }

    public Lexer(Reader<Character> stringReader) {
        this.stringReader = stringReader;
    }

    public List<Token> lex() {
        while (true) {
            if (Character.isJavaIdentifierStart(stringReader.peek())) {
                var sb = new StringBuilder();
                do {
                    sb.append(stringReader.read());
                } while (Character.isJavaIdentifierPart(stringReader.peek()));
                if(isKeyword(sb.toString())) {
                    tokens.add(new Token.Keyword(sb.toString(), stringReader.generateSpan()));
                } else {
                    tokens.add(new Token.IdentifierLiteral(sb.toString(), stringReader.generateSpan()));
                }
            } else if (stringReader.peek() == '"') {
                stringReader.match(it -> it == '"');
                var sb = new StringBuilder();
                while (stringReader.peek() != '"'){
                    sb.append(stringReader.read());
                }
                stringReader.match(it -> it == '"');
                tokens.add(new Token.StringLiteral(sb.toString(), stringReader.generateSpan()));
            } else if (Character.isDigit(stringReader.peek())) {
                var sb = new StringBuilder();
                do {
                    sb.append(stringReader.read());
                } while (Character.isDigit(stringReader.peek()) || stringReader.peek() == '.');

                if (sb.toString().contains(".")) {
                    tokens.add(new Token.FloatingLiteral(Double.parseDouble(sb.toString()), stringReader.generateSpan()));
                } else {
                    tokens.add(new Token.IntegerLiteral(Long.parseLong(sb.toString()), stringReader.generateSpan()));
                }
            } else if (stringReader.peek(0) == ':' && stringReader.peek(1) == ':') {
                stringReader.read();
                stringReader.read();
                tokens.add(new Token.DoubleColon(stringReader.generateSpan()));
            } else if (stringReader.peek(0) == '-' && stringReader.peek(1) == '>') {
                stringReader.read();
                stringReader.read();
                tokens.add(new Token.Arrow(stringReader.generateSpan()));
            } else {
                switch (stringReader.read()) {
                    case '+' -> tokens.add(new Token.Plus(stringReader.generateSpan()));
                    case '-' -> tokens.add(new Token.Minus(stringReader.generateSpan()));
                    case '*' -> tokens.add(new Token.Star(stringReader.generateSpan()));
                    case '/' -> tokens.add(new Token.Slash(stringReader.generateSpan()));
                    case '&' -> tokens.add(new Token.Ampersand(stringReader.generateSpan()));
                    case '|' -> tokens.add(new Token.Line(stringReader.generateSpan()));
                    case ':' -> tokens.add(new Token.Colon(stringReader.generateSpan()));
                    case ',' -> tokens.add(new Token.Comma(stringReader.generateSpan()));
                    case '^' -> tokens.add(new Token.Caret(stringReader.generateSpan()));
                    case '%' -> tokens.add(new Token.Percent(stringReader.generateSpan()));
                    case '$' -> tokens.add(new Token.Dollar(stringReader.generateSpan()));
                    case '>' -> tokens.add(new Token.GreaterThan(stringReader.generateSpan()));
                    case '<' -> tokens.add(new Token.LessThan(stringReader.generateSpan()));
                    case '(' -> tokens.add(new Token.OpenParen(stringReader.generateSpan()));
                    case ')' -> tokens.add(new Token.CloseParen(stringReader.generateSpan()));
                    case '[' -> tokens.add(new Token.OpenBracket(stringReader.generateSpan()));
                    case ']' -> tokens.add(new Token.CloseBracket(stringReader.generateSpan()));
                    case '{' -> tokens.add(new Token.OpenBrace(stringReader.generateSpan()));
                    case '}' -> tokens.add(new Token.CloseBrace(stringReader.generateSpan()));
                    case '=' -> tokens.add(new Token.Equals(stringReader.generateSpan()));
                    case '\n' -> tokens.add(new Token.NewLine(stringReader.generateSpan()));
                    case '\0' -> {
                        return tokens;
                    }
                    case ' ', '\r' -> {}
                    default -> throw new RuntimeException("unknown char at " + stringReader.generateSpan());
                }
            }
        }
    }
}
