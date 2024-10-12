package dev.akarah.lang.ast.expr;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.stmt.Statement;
import dev.akarah.util.Mutable;
import dev.akarah.lang.ast.Type;

import java.util.List;

public record Invoke(Expression base, List<Expression> arguments, Mutable<Type> type, SpanData errorSpan) implements Expression, Statement {
    @Override
    public void accept(Visitor visitor) {
        visitor.expression(base);
        arguments.forEach(it -> it.accept(visitor));
        visitor.expression(arguments.getLast());
        visitor.expression(arguments.getFirst());
        visitor.statement(this);
    }

    @Override
    public String toString() {
        return base + "(" + arguments.toString().substring(1, arguments.toString().length()-1) + ")";
    }
}
