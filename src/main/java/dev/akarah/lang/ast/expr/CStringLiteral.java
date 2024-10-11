package dev.akarah.lang.ast.expr;

import dev.akarah.lang.SpanData;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.nio.charset.StandardCharsets;

public record CStringLiteral(String contents, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(this);
    }

    @Override
    public Mutable<Type> type() {
        return new Mutable<>(
            new Type.CStringPointer(new Type.CArray(new Type.Integer(8), this.contents().getBytes(StandardCharsets.UTF_8).length + 1))
        );
    }
}
