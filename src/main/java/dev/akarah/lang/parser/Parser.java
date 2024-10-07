package dev.akarah.lang.parser;

import dev.akarah.lang.Reader;
import dev.akarah.lang.lexer.Token;
import dev.akarah.lang.ast.AST;
import dev.akarah.lang.tree.Mutable;
import dev.akarah.lang.tree.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

public class Parser {
    Reader<Token> tokenReader;

    public Parser(Reader<Token> tokenReader) {
        this.tokenReader = tokenReader;
    }

    public void skipWhitespace() {
        while (tokenReader.peek() instanceof Token.NewLine)
            tokenReader.read();
    }

    public AST.Program parseAll() {
        var headers = new ArrayList<AST.Header>();

        while (true) {
            skipWhitespace();
            if (tokenReader.peek() instanceof Token.EOF) {
                break;
            }
            headers.add(parseHeader());
            skipWhitespace();
            if (tokenReader.peek() instanceof Token.EOF) {
                break;
            }
        }
        return new AST.Program(headers);
    }

    public AST.Header parseHeader() {
        skipWhitespace();
        if (tokenReader.peek() instanceof Token.Keyword keyword) {
            switch (keyword.keyword()) {
                case "fn" -> {
                    return parseFunction();
                }
                case "struct" -> {
                    throw new RuntimeException("wip");
                }
                case "declare" -> {
                    return parseFunctionDeclaration();
                }
            }
        }
        throw new RuntimeException("??? " + tokenReader.peek());
    }

    public AST.Header.FunctionDeclaration parseFunctionDeclaration() {
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.Keyword kw && kw.keyword().equals("declare"));
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.Keyword kw && kw.keyword().equals("fn"));
        skipWhitespace();
        var ident = (Token.IdentifierLiteral) tokenReader.match(it -> it instanceof Token.IdentifierLiteral);
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.OpenParen);
        var params = new TreeMap<String, Type>();
        loop:
        while (true) {
            skipWhitespace();
            if (tokenReader.peek() instanceof Token.CloseParen) {
                tokenReader.read();
                break;
            }

            var id = (Token.IdentifierLiteral) tokenReader.match(it -> it instanceof Token.IdentifierLiteral);
            tokenReader.match(it -> it instanceof Token.Colon);
            var ty = parseType();

            var ot = tokenReader.match(it -> it instanceof Token.Comma || it instanceof Token.CloseParen);

            params.put(id.literal(), ty);

            switch (ot) {
                case Token.CloseParen closeParen -> {
                    break loop;
                }
                case Token.Comma comma -> {}
                default -> {
                }
            }
        }
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.Arrow);
        skipWhitespace();
        var returnType = parseType();
        skipWhitespace();

        return new AST.Header.FunctionDeclaration(
            ident.literal(),
            params,
            returnType
        );
    }

    public AST.Header.Function parseFunction() {
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.Keyword kw && kw.keyword().equals("fn"));
        skipWhitespace();
        var ident = (Token.IdentifierLiteral) tokenReader.match(it -> it instanceof Token.IdentifierLiteral);
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.OpenParen);

        var params = new TreeMap<String, Type>();
        loop:
        while (true) {
            skipWhitespace();
            if (tokenReader.peek() instanceof Token.CloseParen) {
                tokenReader.read();
                break;
            }

            var id = (Token.IdentifierLiteral) tokenReader.match(it -> it instanceof Token.IdentifierLiteral);
            tokenReader.match(it -> it instanceof Token.Colon);
            var ty = parseType();

            var ot = tokenReader.match(it -> it instanceof Token.Comma || it instanceof Token.CloseParen);

            params.put(id.literal(), ty);

            switch (ot) {
                case Token.CloseParen closeParen -> {
                    break loop;
                }
                case Token.Comma comma -> { continue loop; }
                default -> {
                }
            }
        }
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.Arrow);
        skipWhitespace();
        var returnType = parseType();
        skipWhitespace();
        if (tokenReader.peek() instanceof Token.OpenBrace) {
            return new AST.Header.Function(
                ident.literal(),
                params,
                returnType,
                parseCodeBlock(),
                new Mutable<>()
            );
        } else {
            tokenReader.match(it -> it instanceof Token.Equals);
            return new AST.Header.Function(
                ident.literal(),
                new TreeMap<>(),
                returnType,
                new AST.Expression.CodeBlock(
                    List.of(new AST.Statement.ReturnValue(parseExpression())),
                    new Mutable<>()
                ),
                new Mutable<>()
            );
        }
    }

    public AST.Expression.CodeBlock parseCodeBlock() {
        var stmt = new ArrayList<AST.Statement>();

        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.OpenBrace);
        skipWhitespace();
        while (!(tokenReader.peek() instanceof Token.CloseBrace)) {
            stmt.add(parseStatement());
            tokenReader.match(it -> it instanceof Token.NewLine);
            skipWhitespace();
        }
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.CloseBrace);

        return new AST.Expression.CodeBlock(stmt, new Mutable<>());
    }

    public AST.Statement parseStatement() {
        skipWhitespace();
        return switch (this.tokenReader.peek()) {
            case Token.Keyword kw -> switch (kw.keyword()) {
                case "var" -> {
                    tokenReader.read();

                    var name = (Token.IdentifierLiteral) tokenReader.match(it -> it instanceof Token.IdentifierLiteral);

                    var ty = new Mutable<Type>();
                    if (tokenReader.peek() instanceof Token.Colon) {
                        tokenReader.read();
                        ty.value = parseType();
                    }

                    tokenReader.match(it -> it instanceof Token.Equals);

                    var expr = parseExpression();

                    yield new AST.Statement.VariableDeclaration(name.literal(), ty, expr);
                }
                case "return" -> {
                    tokenReader.read();

                    var expr = parseExpression();
                    yield new AST.Statement.ReturnValue(expr);
                }
                default -> parseExpression();
            };
            default -> parseExpression();
        };
    }

    public AST.Expression parseExpression() {
        return parseFactor();
    }

    public AST.Expression parseFactor() {
        var expr = parseTerm();
        while (true) {
            if (tokenReader.peek() instanceof Token.Star) {
                tokenReader.match(it -> it instanceof Token.Star);
                var rhs = parseTerm();
                expr = new AST.Expression.Mul(expr, rhs, new Mutable<>());
            } else if (tokenReader.peek() instanceof Token.Slash) {
                tokenReader.match(it -> it instanceof Token.Slash);
                var rhs = parseTerm();
                expr = new AST.Expression.Div(expr, rhs, new Mutable<>());
            } else break;
        }
        return expr;
    }

    public AST.Expression parseTerm() {
        var expr = parseNegation();
        while (true) {
            if (tokenReader.peek() instanceof Token.Plus) {
                tokenReader.match(it -> it instanceof Token.Plus);
                var rhs = parseTerm();
                expr = new AST.Expression.Add(expr, rhs, new Mutable<>());
            } else if (tokenReader.peek() instanceof Token.Minus) {
                tokenReader.match(it -> it instanceof Token.Minus);
                var rhs = parseTerm();
                expr = new AST.Expression.Sub(expr, rhs, new Mutable<>());
            } else break;
        }
        return expr;
    }

    public AST.Expression parseNegation() {
        var expr = parsePostfixExpression();
        while (true) {
            if (tokenReader.peek() instanceof Token.Minus) {
                tokenReader.match(it -> it instanceof Token.Minus);
                expr = new AST.Expression.Negate(expr, new Mutable<>());
            } else break;
        }
        return expr;
    }


    public AST.Expression parsePostfixExpression() {
        var expr = parseBaseExpression();
        while (true) {
            if (tokenReader.peek() instanceof Token.OpenBracket op) {
                tokenReader.match(it -> it instanceof Token.OpenBracket);
                var index = parseExpression();
                tokenReader.match(it -> it instanceof Token.CloseBracket);
                expr = new AST.Expression.Subscript(expr, index, new Mutable<>());
            } else if (tokenReader.peek() instanceof Token.OpenParen op) {
                tokenReader.match(it -> it instanceof Token.OpenParen);
                var exprs = new ArrayList<AST.Expression>();
                while (!(tokenReader.peek() instanceof Token.CloseParen)) {
                    exprs.add(parseExpression());
                    if (!(tokenReader.peek() instanceof Token.CloseParen))
                        tokenReader.match(it -> it instanceof Token.Comma);
                }
                tokenReader.match(it -> it instanceof Token.CloseParen);
                expr = new AST.Expression.Invoke(expr, exprs, new Mutable<>());
            } else break;
        }
        return expr;
    }


    public AST.Expression parseBaseExpression() {
        skipWhitespace();
        var t = this.tokenReader.peek();
        return switch (this.tokenReader.read()) {
            case Token.IntegerLiteral il -> new AST.Expression.IntegerLiteral(il.literal(), new Mutable<>());
            case Token.FloatingLiteral fl -> new AST.Expression.FloatingLiteral(fl.literal(), new Mutable<>());
            case Token.OpenBrace ob -> {
                tokenReader.backtrack();
                yield parseCodeBlock();
            }
            case Token.IdentifierLiteral vr -> switch (vr.literal()) {
                case "true" -> new AST.Expression.IntegerLiteral(1, new Mutable<>(new Type.Integer(1)));
                case "false" -> new AST.Expression.IntegerLiteral(0, new Mutable<>(new Type.Integer(1)));
                default -> new AST.Expression.VariableLiteral(vr.literal(), new Mutable<>());
            };
            case Token.OpenParen op -> {
                var inner = parseExpression();
                tokenReader.match(it -> it instanceof Token.CloseParen);
                yield inner;
            }
            case Token.StringLiteral sl -> {
                yield new AST.Expression.CStringLiteral(sl.literal());
            }
            case Token.Keyword keyword -> {
                switch (keyword.keyword()) {
                    case "if" -> {

                        var condition = parseExpression();
                        var ifTrueBlock = parseCodeBlock();

                        if (tokenReader.peek() instanceof Token.Keyword tk && tk.keyword().equals("else")) {
                            tokenReader.read();
                            yield new AST.Expression.Conditional(
                                condition,
                                ifTrueBlock,
                                Optional.of(parseCodeBlock()),
                                new Mutable<>()
                            );
                        } else {
                            yield new AST.Expression.Conditional(
                                condition,
                                ifTrueBlock,
                                Optional.empty(),
                                new Mutable<>()
                            );
                        }
                    }
                    default -> throw new RuntimeException("bad at " + keyword);
                }
            }
            default -> throw new RuntimeException("not an expression " + t);
        };
    }

    public Type parseType() {
        skipWhitespace();
        return parseType$1();
    }

    public Type parseType$1() {
        skipWhitespace();
        return switch (tokenReader.peek()) {
            case Token.Ampersand ampersand -> {
                tokenReader.read();
                yield new Type.Reference(parseType());
            }
            default -> parseType$2();
        };
    }

    public Type parseType$2() {
        var base = parseType$3();
        while(true) {
            if(tokenReader.peek() instanceof Token.OpenBracket) {
                tokenReader.match(it -> it instanceof Token.OpenBracket);
                var o = tokenReader.match(it -> it instanceof Token.IntegerLiteral || it instanceof Token.CloseBracket);
                if(o instanceof Token.IntegerLiteral il) {
                    tokenReader.match(it -> it instanceof Token.CloseBracket);
                    base = new Type.Array(base, il.literal());
                } else {
                    base = new Type.Array(base, -1);
                }
            } else break;
        }
        return base;
    }

    public Type parseType$3() {
        skipWhitespace();
        return switch (tokenReader.read()) {
            case Token.Keyword kw -> switch (kw.keyword()) {
                case "f32" -> new Type.F32();
                case "f64" -> new Type.F64();
                case "f128" -> new Type.F128();
                case "bool" -> new Type.Integer(1);
                default -> throw new IllegalStateException("Unexpected value: " + kw.keyword());
            };
            case Token.IdentifierLiteral identifierLiteral -> {
                if (identifierLiteral.literal().startsWith("i")) {
                    yield new Type.Integer(Integer.parseInt(identifierLiteral.literal().replaceFirst("i", "")));
                }
                if (identifierLiteral.literal().startsWith("u")) {
                    yield new Type.UnsignedInteger(Integer.parseInt(identifierLiteral.literal().replaceFirst("u", "")));
                }
                throw new IllegalStateException("Unexpected value: " + identifierLiteral);
            }
            default -> throw new IllegalStateException("Unexpected value: " + tokenReader.peek());
        };
    }
}
