package dev.akarah.util;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.error.CompileError;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Reader<T> {
    default T peek() {
        return this.peek(0);
    }
    T peek(int amount);
    T read();
    T match(Predicate<T> predicate, Consumer<T> runOnFailure);
    default T match(Predicate<T> predicate) {
        return this.match(predicate, (it) -> { throw new CompileError.RawMessage("Some assertion failed with " + it, this.generateSpan()); });
    }
    default T match(Predicate<T> predicate, CompileError compileError) {
        return this.match(predicate, (it) -> { throw compileError; });
    }
    SpanData generateSpan();
    void backtrack(int amount);
    default void backtrack() {
        backtrack(0);
    }
}
