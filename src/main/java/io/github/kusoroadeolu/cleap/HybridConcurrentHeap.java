package io.github.kusoroadeolu.cleap;

public class HybridConcurrentHeap<T extends Comparable<T>> implements Heap<T> {
    @Override
    public boolean insert(T t) {
        return false;
    }

    @Override
    public T findMax() {
        return null;
    }

    @Override
    public T extractMax() {
        return null;
    }
}

