package dev.akarah.lang.ast.header;

import dev.akarah.lang.ast.expr.Expression;

import java.util.List;

public record Attribute(
    String name,
    List<Expression> arguments
) {
}
