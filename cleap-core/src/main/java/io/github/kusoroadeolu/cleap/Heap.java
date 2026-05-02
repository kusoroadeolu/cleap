package io.github.kusoroadeolu.cleap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

public interface Heap<T extends Comparable<T>> extends Queue<T> {
    boolean insert(T t);

    T peek();

    T poll();

    int size();

    int capacity();

    @Override
    default boolean add(T t) {
        return false;
    }

    @Override
    default boolean offer(T t) {
        return false;
    }

    @Override
    default T remove() {
        return null;
    }

    @Override
    default T element() {
        return null;
    }

    @Override
    default boolean isEmpty() {
        return false;
    }

    @Override
    default boolean contains(Object o) {
        return false;
    }

    @Override
    default Iterator<T> iterator() {
        return null;
    }

    @Override
    default Object[] toArray() {
        return new Object[0];
    }

    @Override
    default <T1> T1[] toArray(T1[] a) {
        return null;
    }

    @Override
    default boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    default boolean addAll(Collection<? extends T> c) {
        return false;
    }

    @Override
    default boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    default boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    default void clear() {

    }
}
