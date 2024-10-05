package dev.akarah.lang.tree;

public class Mutable<T> {
    public T v;

    public Mutable() {
        this.v = null;
    }

    public Mutable(T value) {
        this.v = value;
    }

    public String toString() {
        if(this.v == null)
            return "null";
        return this.v.toString();
    }
}
