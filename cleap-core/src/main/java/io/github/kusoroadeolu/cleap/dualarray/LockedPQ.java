package io.github.kusoroadeolu.cleap.dualarray;

import io.github.kusoroadeolu.cleap.PriorityQueue;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockedPQ<T> implements PriorityQueue<T> {
    final Lock lock = new ReentrantLock();
    final java.util.PriorityQueue<T> queue = new java.util.PriorityQueue<>();
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
