package dev.akarah.lang.lexer;

import dev.akarah.lang.SpanData;

public sealed interface Token {
    record Keyword(String keyword, SpanData span) implements Token {}
    record IdentifierLiteral(String literal, SpanData span) implements Token {}
    record IntegerLiteral(long literal, SpanData span) implements Token {}
    record StringLiteral(String literal, SpanData span) implements Token {}
    record FloatingLiteral(double literal, SpanData span) implements Token {}
    record NewLine(SpanData span) implements Token {}

    record OpenParen(SpanData span) implements Token {}
    record CloseParen(SpanData span) implements Token {}
    record OpenBrace(SpanData span) implements Token {}
    record CloseBrace(SpanData span) implements Token {}
    record OpenBracket(SpanData span) implements Token {}
    record CloseBracket(SpanData span) implements Token {}

    record LessThan(SpanData span) implements Token {}
    record GreaterThan(SpanData span) implements Token {}

    record Equals(SpanData span) implements Token {}

    record Colon(SpanData span) implements Token {}

    // ::
    record DoubleColon(SpanData span) implements Token {}

    record Period(SpanData span) implements Token {}
    record Comma(SpanData span) implements Token {}

    record Plus(SpanData span) implements Token {}
    record Minus(SpanData span) implements Token {}
    record Star(SpanData span) implements Token {}
    record Slash(SpanData span) implements Token {}
    record Caret(SpanData span) implements Token {}
    record Percent(SpanData span) implements Token {}
    record Dollar(SpanData span) implements Token {}
    record Ampersand(SpanData span) implements Token {}
    record Line(SpanData span) implements Token {}
    record EOF(SpanData span) implements Token {}

    // ->
    record Arrow(SpanData span) implements Token {}

    SpanData span();
}
