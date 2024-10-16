package dev.akarah.lang.ast;

import dev.akarah.lang.SpanData;
import dev.akarah.lang.ast.header.Header;

import java.util.ArrayList;
import java.util.List;

public record Program(
    List<Header> headers
) implements AST {
    public Program join(Program other) {
        var list = new ArrayList<Header>(headers);
        list.addAll(other.headers);
        return new Program(list);
    }

    public SpanData errorSpan() {
        return null;
    }
}
