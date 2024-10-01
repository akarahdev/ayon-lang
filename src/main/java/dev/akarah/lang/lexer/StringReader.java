package dev.akarah.lang.lexer;

import dev.akarah.lang.Reader;
import dev.akarah.lang.SpanData;

import java.util.function.Predicate;

public class StringReader implements Reader<Character> {
    String string;
    int index = -1;

    public StringReader(String string, String fileName) {
        this.string = string;
        this.fileName = fileName;
    }

    String fileName;
    int line = 1;
    int column = 0;

    @Override
    public Character peek(int amount) {
        try {
            return string.charAt(index + amount + 1);
        } catch (StringIndexOutOfBoundsException exception) {
            return '\0';
        }
    }

    @Override
    public Character read() {
        try {
            column++;
            if (peek() == '\n') {
                column = 0;
                line++;
            }
            return string.charAt(++index);
        } catch (StringIndexOutOfBoundsException exception) {
            return '\0';
        }
    }

    @Override
    public Character match(Predicate<Character> predicate) {
        var r = read();
        if (!predicate.test(r)) {
            throw new RuntimeException("Predicate " + predicate + " failed with " + r);
        }
        return r;
    }

    @Override
    public SpanData generateSpan() {
        return new SpanData(
            fileName,
            line,
            column
        );
    }

    @Override
    public void backtrack(int amount) {
        this.index -= amount + 1;
    }
}
