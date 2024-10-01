package dev.akarah.lang;

import dev.akarah.lang.lexer.Token;

import java.util.function.Predicate;

public interface Reader<T> {
    default T peek() {
        return this.peek(0);
    }
    T peek(int amount);
    T read();
    T match(Predicate<T> predicate);
    SpanData generateSpan();
    void backtrack(int amount);
    default void backtrack() {
        backtrack(0);
    }
}
