package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.Heap;

import java.util.concurrent.PriorityBlockingQueue;

public class JDKHeap<T> implements Heap<T> {

    private final PriorityBlockingQueue<T> queue;

    public JDKHeap(PriorityBlockingQueue<T> queue) {
        this.queue = queue;
    }


    public JDKHeap() {
        this.queue = new PriorityBlockingQueue<>();
    }



    @Override
    public boolean add(T t) {
        return queue.offer(t);
    }

    @Override
    public T peek() {
        return queue.peek();
    }

    @Override
    public T poll() {
        return queue.poll();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public void clear() {
        queue.clear();
    }
}
