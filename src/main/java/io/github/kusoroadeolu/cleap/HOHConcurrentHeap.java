package io.github.kusoroadeolu.cleap;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HOHConcurrentHeap<T extends Comparable<T>> implements Heap<T> {
    private final Node[] items;
    private final int capacity;
    private final Lock sizeLock;
    private int size;

    //A bounded concurrent HOH heap
    public HOHConcurrentHeap(int capacity) {
        sizeLock = new ReentrantLock();
        int pow2 = 1 << (32 - Integer.numberOfLeadingZeros(capacity - 1));
        items = new Node[this.capacity = (pow2 - 1)];
        Arrays.fill(items, new Node<>(null));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean insert(T t) {
//        Objects.requireNonNull(t);
//        int idx = incrementAndGetSize();
//        if (idx > capacity) return false;
//        long pid = Thread.currentThread().threadId();
//        Node<T> ours = (Node<T>) items[idx];
//        setValueAndState(ours, t, pid);
//        int pIdx = parentIndex(idx);
//        if (pIdx < 0) return true; //We're the head
//
//        Node<T> parent = (Node<T>) items[pIdx];
//        boolean hasParentLock = false;
//        while (pIdx > 0) {
//            int cIdx1 = childIndex(pIdx, 1);
//            int cIdx2 = childIndex(pIdx, 2);
//            parent.lock();
//            if (parent.state == Node.NULL) {
//                hasParentLock = false;
//                parent.unlock();
//            }else if (parent.state == pid) {
//                hasParentLock = false;
//                parent.unlock();
//            }else if (parent.state != Node.AVAILABLE) {
//                hasParentLock = false;
//                parent.unlock();
//            } else hasParentLock = true;
//
//            Node<T> left = itemAt(cIdx1);
//            Node<T> right = itemAt(cIdx2);
//            left.lock();
//            right.lock();
//
//            if (left.state == pid) {
//                right.unlock();
//            }
//            else if (right.state == pid) {
//                left.unlock();
//            }
//            else left.unlock(); right.unlock();
//        }
    }

    Node<T> itemAt(int i) {
        return  (Node<T>) items[i];
    }

    int incrementAndGetSize () {
        Lock l = sizeLock;
        try {
            l.lock();
            return size++;
        }finally {
            l.unlock();;
        }
    }

    void setValueAndState(Node<T> node, T value, long state) {
        try {
            node.lock();
            node.state = state;
            node.value = value;
        }finally {
            node.unlock();
        }
    }

    private int childIndex(int parentIdx, int by) {
        return parentIdx * 2 + by;
    }

    private int parentIndex(int childIdx) {
        return (childIdx - 1 ) / 2;
    }

    T valueAtHead() {
        var v = itemAt(0);
        try {
           v.lock();
           return v.value;
        }finally {
            v.unlock();
        }
    }


    @Override
    public T peek() {
        return valueAtHead();
    }

    /*
        Node n
    * Hold the size lock
    *   size = this.size;
    *   if(size = 0) return null;
    *   Hold the itemAt(0) lock
    *       n = itemAt(0)
            n.state = EMPTY;
            n.value = null; //Null the node's value
    *       if(this.size-- = 1) {
    *           Release the n lock
    *           Release the size lock
    *           return n.value;
    *       }
    *       Node lastNonNullNode = itemAt(size - 1);
    *       Hold lNNN lock
    *       item[0] = lNNN
    *       item[size - 1] = n;
    *   Release the lNNN lock
    * Release the size lock
    *
    * Here we should still hold the lock for n
      int pIdx = 0;
      Repeatedly:
      n.unlock()
      Node c1 = itemAt(findChildIdx(pIdx))
      Node c2 = itemAt(findChildIdx(pIdx))
      c1.lock(); c2.lock()
      if(c1 && c2 == null) {
        c2.unlock();
        c1.unlock();
        return value;
      }

      if(c1.compareTo(n) > 0) {
        c2.unlock()
      } else  {
        c1.unlock();
      }

      swap(n, c1, n(pIdx), c1(curr))

        n = c1/c2; //N = c1/c2, we'll use a temp var here
        pIdx = cIdx1/cIdx2

    * */
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

    static class Node<T extends Comparable<T>> {
        T value;
        private final Lock nodeLock;
        long state = EMPTY;
        static final int EMPTY = -2;
        static final int AVAILABLE = -1;

        public Node(T value) {
            this.value = value;
            this.nodeLock = new ReentrantLock();
        }

        void lock() {
            nodeLock.lock();
        }

        void unlock() {
            nodeLock.unlock();
        }
    }
}

