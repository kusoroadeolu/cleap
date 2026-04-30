package io.github.kusoroadeolu.cleap;

public interface Heap<T extends Comparable<T>> {
    boolean insert(T t);

    T peek();

    T head();

    int size();

    int capacity();
}
