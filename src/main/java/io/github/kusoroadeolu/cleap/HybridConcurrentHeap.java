package io.github.kusoroadeolu.cleap;

public class HybridConcurrentHeap<T extends Comparable<T>> implements Heap<T> {
    @Override
    public boolean insert(T t) {
        return false;
    }

    @Override
    public T peek() {
        return null;
    }

    @Override
    public T head() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public int capacity() {
        return 0;
    }
}

