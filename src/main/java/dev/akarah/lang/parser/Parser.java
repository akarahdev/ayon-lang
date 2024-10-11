package dev.akarah.lang.parser;

import dev.akarah.lang.ast.Program;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.block.CodeBlock;
import dev.akarah.lang.ast.block.CodeBlockData;
import dev.akarah.lang.ast.expr.*;
import dev.akarah.lang.ast.header.*;
import dev.akarah.lang.ast.stmt.IfStatement;
import dev.akarah.lang.ast.stmt.ReturnValue;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.lang.ast.stmt.VariableDeclaration;
import dev.akarah.lang.error.CompileError;
import dev.akarah.lang.lexer.Token;
import dev.akarah.util.Mutable;
import dev.akarah.util.Reader;

import java.util.*;

public class Parser {
    Reader<Token> tokenReader;

    public Parser(Reader<Token> tokenReader) {
        this.tokenReader = tokenReader;
    }

    public void skipWhitespace() {
        while (tokenReader.peek() instanceof Token.NewLine)
            tokenReader.read();
    }

    public Program parseAll() {
        var headers = new ArrayList<Header>();

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
        return new Program(headers);
    }

    public Attribute parseAttribute() {
        var at = tokenReader.match(it -> it instanceof Token.At);
        var ident = parseIdentifier();
        var args = new ArrayList<Expression>();
        if(tokenReader.peek() instanceof Token.OpenParen) {
            tokenReader.match(it -> it instanceof Token.OpenParen);
            skipWhitespace();
            while(true) {
                var expr = parseExpression();
                args.add(expr);
                if(!(tokenReader.peek() instanceof Token.Comma))
                    break;
            }
            tokenReader.match(it -> it instanceof Token.CloseParen);
            skipWhitespace();
        }
        return new Attribute(
            ident,
            args
        );
    }

    public Header parseHeader() {
        skipWhitespace();
        var attrs = new ArrayList<Attribute>();
        skipWhitespace();
        while(tokenReader.peek() instanceof Token.At) {
            attrs.add(parseAttribute());
            skipWhitespace();
        }
        if (tokenReader.peek() instanceof Token.Keyword keyword) {
            switch (keyword.keyword()) {
                case "fn" -> {
                    return parseFunction(attrs);
                }
                case "record" -> {
                    return parseStructureDeclaration(attrs);
                }
                case "declare" -> {
                    return parseFunctionDeclaration(attrs);
                }
            }
        }
        throw new RuntimeException("??? " + tokenReader.peek());
    }

    public StructureDeclaration parseStructureDeclaration(List<Attribute> attributes) {
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.Keyword kw && kw.keyword().equals("record"));
        var name = parseIdentifier();

        var parameters = new LinkedHashMap<String, Type>();
        tokenReader.match(it -> it instanceof Token.OpenBrace);
        skipWhitespace();

        while (tokenReader.peek() instanceof Token.IdentifierLiteral il) {
            skipWhitespace();
            tokenReader.read();
            var field = il.literal();
            tokenReader.match(it -> it instanceof Token.Colon);
            var ty = parseType();
            parameters.put(field, ty);
            skipWhitespace();
        }
        tokenReader.match(it -> it instanceof Token.CloseBrace);

        return new StructureDeclaration(
            name,
            parameters,
            attributes
        );

    }

    public FunctionDeclaration parseFunctionDeclaration(List<Attribute> attributes) {
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.Keyword kw && kw.keyword().equals("declare"));
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.Keyword kw && kw.keyword().equals("fn"));
        skipWhitespace();
        var ident = parseIdentifier();
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.OpenParen);
        var params = new LinkedHashMap<String, Type>();
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

            var ot = tokenReader.match(it -> it instanceof Token.Comma || it instanceof Token.CloseParen,
                it -> { throw new CompileError.UnexpectedTokens(it, Token.Comma.class, Token.CloseParen.class); });

            params.put(id.literal(), ty);

            switch (ot) {
                case Token.CloseParen closeParen -> {
                    break loop;
                }
                case Token.Comma comma -> {
                }
                default -> {
                }
            }
        }
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.Arrow,
            it -> { throw new CompileError.UnexpectedTokens(it, Token.Arrow.class); });
        skipWhitespace();
        var returnType = parseType();
        skipWhitespace();

        return new FunctionDeclaration(
            ident,
            params,
            returnType,
            attributes
        );
    }

    public Function parseFunction(List<Attribute> attributes) {
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.Keyword kw && kw.keyword().equals("fn"));
        skipWhitespace();
        var ident = parseIdentifier();
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.OpenParen,
            it -> { throw new CompileError.UnexpectedTokens(it, Token.OpenParen.class); });

        var params = new LinkedHashMap<String, Type>();
        loop:
        while (true) {
            skipWhitespace();
            if (tokenReader.peek() instanceof Token.CloseParen) {
                tokenReader.read();
                break;
            }

            var id = (Token.IdentifierLiteral) tokenReader.match(it -> it instanceof Token.IdentifierLiteral,
                it -> { throw new CompileError.UnexpectedTokens(it, Token.IdentifierLiteral.class); });
            tokenReader.match(it -> it instanceof Token.Colon);
            var ty = parseType();

            var ot = tokenReader.match(it -> it instanceof Token.Comma || it instanceof Token.CloseParen,
                it -> { throw new CompileError.UnexpectedTokens(it, Token.Comma.class, Token.CloseParen.class); });

            params.put(id.literal(), ty);

            switch (ot) {
                case Token.CloseParen closeParen -> {
                    break loop;
                }
                case Token.Comma comma -> {
                    continue loop;
                }
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
            return new Function(
                ident,
                params,
                returnType,
                parseCodeBlock(),
                attributes
            );
        } else {
            tokenReader.match(it -> it instanceof Token.Equals);
            return new Function(
                ident,
                new LinkedHashMap<>(),
                returnType,
                new CodeBlock(
                    List.of(new ReturnValue(parseExpression())),
                    new CodeBlockData(new HashMap<>(), new HashMap<>())
                ),
                attributes
            );
        }
    }

    public CodeBlock parseCodeBlock() {
        var stmt = new ArrayList<Statement>();

        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.OpenBrace,
            it -> { throw new CompileError.UnexpectedTokens(it, Token.OpenBrace.class); });
        skipWhitespace();
        while (!(tokenReader.peek() instanceof Token.CloseBrace)) {
            stmt.add(parseStatement());
            tokenReader.match(it -> it instanceof Token.NewLine,
                it -> { throw new CompileError.UnexpectedTokens(it, Token.NewLine.class); });
            skipWhitespace();
        }
        skipWhitespace();
        tokenReader.match(it -> it instanceof Token.CloseBrace,
            it -> { throw new CompileError.UnexpectedTokens(it, Token.CloseBrace.class); });

        return new CodeBlock(stmt, new CodeBlockData(new HashMap<>(), new HashMap<>()));
    }

    public Statement parseStatement() {
        skipWhitespace();
        return switch (this.tokenReader.peek()) {
            case Token.Keyword kw -> switch (kw.keyword()) {
                case "if" -> {
                    tokenReader.read();

                    var cond = parseExpression();
                    var ifTrue = parseCodeBlock();
                    if (tokenReader.peek() instanceof Token.Keyword tk && tk.keyword().equals("else")) {
                        tokenReader.read();
                        var ifFalse = parseCodeBlock();
                        yield new IfStatement(cond, ifTrue, ifFalse);
                    } else {
                        yield new IfStatement(cond, ifTrue, new CodeBlock(List.of(), new CodeBlockData(new HashMap<>(), new HashMap<>())));
                    }
                }
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

                    yield new VariableDeclaration(name.literal(), ty, expr);
                }
                case "return" -> {
                    tokenReader.read();

                    var expr = parseExpression();
                    yield new ReturnValue(expr);
                }
                default -> parseExpression();
            };
            default -> parseExpression();
        };
    }

    public Expression parseExpression() {
        return parseStorage();
    }

    public Expression parseStorage() {
        var expr = parseFactor();
        while (true) {
            if (tokenReader.peek() instanceof Token.Equals) {
                tokenReader.match(it -> it instanceof Token.Equals);
                var rhs = parseTerm();
                expr = new Store(expr, rhs, new Mutable<>());
            } else break;
        }
        return expr;
    }


    public Expression parseFactor() {
        var expr = parseTerm();
        while (true) {
            if (tokenReader.peek() instanceof Token.Star) {
                tokenReader.match(it -> it instanceof Token.Star);
                var rhs = parseTerm();
                expr = new Mul(expr, rhs, new Mutable<>());
            } else if (tokenReader.peek() instanceof Token.Slash) {
                tokenReader.match(it -> it instanceof Token.Slash);
                var rhs = parseTerm();
                expr = new Div(expr, rhs, new Mutable<>());
            } else break;
        }
        return expr;
    }

    public Expression parseTerm() {
        var expr = parseNegation();
        while (true) {
            if (tokenReader.peek() instanceof Token.Plus) {
                tokenReader.match(it -> it instanceof Token.Plus);
                var rhs = parseTerm();
                expr = new Add(expr, rhs, new Mutable<>());
            } else if (tokenReader.peek() instanceof Token.Minus) {
                tokenReader.match(it -> it instanceof Token.Minus);
                var rhs = parseTerm();
                expr = new Sub(expr, rhs, new Mutable<>());
            } else break;
        }
        return expr;
    }

    public Expression parseNegation() {
        var expr = parseUfcs();
        while (true) {
            if (tokenReader.peek() instanceof Token.Minus) {
                tokenReader.match(it -> it instanceof Token.Minus);
                expr = new Negate(expr, new Mutable<>());
            } else break;
        }
        return expr;
    }

    public Expression parseUfcs() {
        var expr = parsePostfixExpression();
        while (true) {
            if (tokenReader.peek() instanceof Token.Period period) {
                tokenReader.read();
                var lhs = parseExpression();
                if (lhs instanceof Invoke invoke) {
                    invoke.arguments().addFirst(expr);
                    expr = invoke;
                } else {
                    throw new RuntimeException(lhs + " must be an invocation trust me bro");
                }
            } else if (tokenReader.peek() instanceof Token.Arrow arrow) {

            } else {
                break;
            }
        }
        System.out.println(expr);
        return expr;
    }

    public Expression parsePostfixExpression() {
        var expr = parseBaseExpression();
        while (true) {
            if (tokenReader.peek() instanceof Token.Arrow arrow) {
                tokenReader.match(it -> it instanceof Token.Arrow);
                var field = parseIdentifier();
                expr = new FieldAccess(expr, field, new Mutable<>());
            } else if (tokenReader.peek() instanceof Token.OpenBracket op) {
                tokenReader.match(it -> it instanceof Token.OpenBracket);
                var index = parseExpression();
                tokenReader.match(it -> it instanceof Token.CloseBracket);
                expr = new Subscript(expr, index, new Mutable<>());
            } else if (tokenReader.peek() instanceof Token.OpenParen op) {
                tokenReader.match(it -> it instanceof Token.OpenParen);
                var exprs = new ArrayList<Expression>();
                skipWhitespace();
                while (!(tokenReader.peek() instanceof Token.CloseParen)) {
                    skipWhitespace();
                    exprs.add(parseExpression());
                    skipWhitespace();
                    if (!(tokenReader.peek() instanceof Token.CloseParen))
                        tokenReader.match(it -> it instanceof Token.Comma);
                }
                tokenReader.match(it -> it instanceof Token.CloseParen);
                expr = new Invoke(expr, exprs, new Mutable<>());
            } else if (tokenReader.peek() instanceof Token.Keyword kw && kw.keyword().equals("as")) {
                tokenReader.match(it -> it instanceof Token.Keyword);
                var ty = parseType();
                expr = new BitCast(expr, new Mutable<>(ty));
            } else break;
        }
        return expr;
    }


    public Expression parseBaseExpression() {
        skipWhitespace();
        var t = this.tokenReader.peek();
        return switch (this.tokenReader.read()) {
            case Token.IntegerLiteral il -> new IntegerLiteral(il.literal(), new Mutable<>());
            case Token.FloatingLiteral fl -> new FloatingLiteral(fl.literal(), new Mutable<>());
            case Token.Keyword kw -> switch (kw.keyword()) {
                case "true" -> new IntegerLiteral(1, new Mutable<>(new Type.Integer(1)));
                case "false" -> new IntegerLiteral(0, new Mutable<>(new Type.Integer(1)));
                case "init" -> {
                    yield new InitStructure(new Mutable<>(parseType()));
                }
                default -> throw new RuntimeException("uuhhh");
            };
            case Token.IdentifierLiteral vr -> {
                tokenReader.backtrack();
                var parsed = parseIdentifier();
                yield new VariableLiteral(parsed, new Mutable<>());
            }
            case Token.OpenParen op -> {
                var inner = parseExpression();
                tokenReader.match(it -> it instanceof Token.CloseParen);
                yield inner;
            }
            case Token.StringLiteral sl -> {
                yield new CStringLiteral(sl.literal());
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
        while (true) {
            if (tokenReader.peek() instanceof Token.OpenBracket) {
                tokenReader.match(it -> it instanceof Token.OpenBracket);
                var o = tokenReader.match(it -> it instanceof Token.IntegerLiteral || it instanceof Token.CloseBracket);
                if (o instanceof Token.IntegerLiteral il) {
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
                case "void" -> new Type.Unit();
                default -> throw new IllegalStateException("Unexpected value: " + kw.keyword());
            };
            case Token.IdentifierLiteral identifierLiteral -> {
                tokenReader.backtrack();
                var literal = parseIdentifier();
                try {
                    if (literal.startsWith("i")) {
                        yield new Type.Integer(Integer.parseInt(identifierLiteral.literal().replaceFirst("i", "")));
                    } else if (literal.startsWith("u")) {
                        yield new Type.UnsignedInteger(Integer.parseInt(identifierLiteral.literal().replaceFirst("u", "")));
                    } else {
                        yield new Type.UserStructure(literal);
                    }
                } catch (NumberFormatException exception) {
                    yield new Type.UserStructure(literal);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + tokenReader.peek());
        };
    }

    public String parseIdentifier() {
        var literal = (Token.IdentifierLiteral) tokenReader.match(it -> it instanceof Token.IdentifierLiteral);
        if (tokenReader.peek() instanceof Token.DoubleColon doubleColon) {
            tokenReader.read();
            return literal.literal() + "::" + parseIdentifier();
        }
        return literal.literal();
    }
}
