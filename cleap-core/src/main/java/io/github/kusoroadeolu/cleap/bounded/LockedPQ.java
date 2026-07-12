package io.github.kusoroadeolu.cleap.bounded;

import io.github.kusoroadeolu.cleap.Heap;

import java.lang.invoke.VarHandle;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockedPQ<T> implements Heap<T> {
    final Lock lock = new ReentrantLock();
    final PriorityQueue<T> queue = new PriorityQueue<>();
    final int capacity;

    public LockedPQ(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean add(T t) {
        var l = lock;
        var q = queue;
        l.lock();
        try {
            int size = q.size();
            if (size == capacity) return false;
            else return q.add(t);
        }finally {
            l.unlock();
        }
    }

    @Override
    public T peek() {
        return null;
    }

    @Override
    public T poll() {
        var l = lock;
        var q = queue;
        l.lock();
        try {
            return q.poll();
        }finally {
            l.unlock();
        }
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void clear() {
        var l = lock;
        l.lock();
        try {
            queue.clear();
        }finally {
            l.unlock();
        }
    }
}
