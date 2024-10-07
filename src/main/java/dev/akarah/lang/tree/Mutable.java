package dev.akarah.lang.tree;

public class Mutable<T> {
    public T value;

    public Mutable<T> set(T value) {
        this.value = value;
        return this;
    }

    public T get() {
        return this.value;
    }

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
