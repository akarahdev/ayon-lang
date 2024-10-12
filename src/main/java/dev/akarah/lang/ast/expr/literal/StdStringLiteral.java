package dev.akarah.lang.ast.expr.literal;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.Type;
import dev.akarah.lang.ast.expr.Expression;
import dev.akarah.util.Mutable;

public record StdStringLiteral(String contents, SpanData errorSpan) implements Expression {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(this);
    }

    @Override
    public Mutable<Type> type() {
        return new Mutable<>(new Type.UserStructure("std::string"));
    }

    @Override
    public String toString() {
        return '"' + contents + "\\\\00" + '"';
    }
}
