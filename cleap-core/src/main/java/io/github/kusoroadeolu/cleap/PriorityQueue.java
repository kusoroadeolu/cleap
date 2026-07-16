package io.github.kusoroadeolu.cleap;

import java.util.ArrayList;
import java.util.List;

public interface PriorityQueue<T> {
    boolean add(T t);

    T poll();

    int size();

    default void clear() {
        while (poll() != null) {}
    }

    default List<T> drain() {
        List<T> sink = new ArrayList<>();
        T t;

        while ((t = poll()) != null) {
            sink.add(t);
        }

        return sink;
    }
}
