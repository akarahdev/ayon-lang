package dev.akarah.lang.parser;

import dev.akarah.util.Reader;
import dev.akarah.lang.SpanData;
import dev.akarah.lang.lexer.Token;

import java.util.List;
import java.util.function.Predicate;

public class TokenReader implements Reader<Token> {
    List<Token> tokens;
    int index = -1;

    public TokenReader(List<Token> tokens) {
        this.tokens = tokens;
    }

    @Override
    public Token peek(int amount) {
        try {
            return tokens.get(index+1+amount);
        } catch (IndexOutOfBoundsException ex) {
            return new Token.EOF(tokens.getLast().span());
        }
    }

    @Override
    public Token read() {
        try {
            return tokens.get(++index);
        } catch (IndexOutOfBoundsException ex) {
            return new Token.EOF(tokens.getLast().span());
        }
    }

    @Override
    public Token match(Predicate<Token> predicate) {
        var r = read();
        if(!predicate.test(r)) {
            throw new RuntimeException("Predicate " + predicate + " failed for " + r);
        }
        return r;
    }

    @Override
    public SpanData generateSpan() {
        return this.peek().span();
    }

    @Override
    public void backtrack(int amount) {
        this.index -= amount + 1;
    }
}
