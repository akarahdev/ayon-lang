package dev.akarah.util;

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
            return "?";
        return this.value.toString();
    }
}
