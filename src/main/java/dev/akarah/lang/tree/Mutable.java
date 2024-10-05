package dev.akarah.lang.tree;

import java.util.Optional;

public class Mutable<T> {
    public T value;

    public Mutable() {
        this.value = null;
    }

    public Mutable(T value) {
        this.value = value;
    }

    public String toString() {
        if(this.value == null)
            return "null";
        return this.value.toString();
    }
}
