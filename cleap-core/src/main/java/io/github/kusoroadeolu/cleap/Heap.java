package io.github.kusoroadeolu.cleap;

public interface Heap<T> {
    boolean add(T t);

    T peek();

    T poll();

    int size();

    default void clear() {}
}
