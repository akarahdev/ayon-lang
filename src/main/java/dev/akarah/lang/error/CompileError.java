package dev.akarah.lang.error;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.lexer.Token;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CompileError extends RuntimeException {
    public static class RawMessage extends CompileError {
        String message;
        SpanData span;

        public RawMessage(String message, SpanData span) {
            this.message = message;
            this.span = span;
        }

        @Override
        public String toErrorMessage() {
            return this.message;
        }

        @Override
        public SpanData span() {
            return this.span;
        }
    }

    public static class UnexpectedCharacter extends CompileError {
        String message;
        SpanData span;

        public UnexpectedCharacter(String message, SpanData span) {
            this.message = message;
            this.span = span;
        }

        @Override
        public String toErrorMessage() {
            return "unexpected character: " + this.message;
        }

        @Override
        public SpanData span() {
            return this.span;
        }
    }

    public static class UnexpectedTokens extends CompileError {
        Token found;
        Class<? extends Token>[] expected;
        SpanData span;

        @SafeVarargs
        public UnexpectedTokens(Token found, Class<? extends Token>... expected) {
            this.found = found;
            this.expected = expected;
            this.span = found.span();
        }

        @Override
        public String toErrorMessage() {
            return "found " + found.getClass() + ", expected " +
                Arrays.stream(expected)
                    .map(it -> it.getName())
                    .collect(Collectors.joining(", "));
        }

        @Override
        public SpanData span() {
            return this.span;
        }
    }

    public abstract String toErrorMessage();
    public abstract SpanData span();
}
