package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.PriorityQueue;

import java.util.concurrent.PriorityBlockingQueue;

public class JDKPriorityQueue<T> implements PriorityQueue<T> {

    private final PriorityBlockingQueue<T> queue;

    public JDKPriorityQueue(PriorityBlockingQueue<T> queue) {
        this.queue = queue;
    }


    public JDKPriorityQueue() {
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
