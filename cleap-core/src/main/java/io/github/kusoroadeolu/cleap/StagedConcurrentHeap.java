package io.github.kusoroadeolu.cleap;


import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * An unbounded optimistic concurrent MAX heap.
 * Peek, Head(Poll) and insert operations in this list are protected through mutual exclusion.
 *
 * A dual data structure approach is used for this, a MPSC concurrent stack and a sequential(non thread safe) priority queue/heap (could be array or node based). We also keep an atomic integer field for incrementing the size to avoid a size lock
 * Each node in MPSC stack contains a value to be inserted to the priority queue and a next node pointer
 * If a node is alive it should be added to the priority queue otherwise it should not
 *
 * We have two options here, increment the size at the queue CAS site or increment the size when applying. Either implies the linearizability point of this queue
 * Insert:
 *   Initialize a new node object with our item on construction
 *   Then repeatedly try to cas our node to the head of the stack until we succeed
 *   Then try to acquire the lock, if we fail, return our result as true (since this heap is unbounded)
 *   Otherwise
 *       Detach the stack from its head using an atomic getSet instruction, setting the current head as null
 *       Store a count variable
 *       Repeatedly
 *           If the node is null
 *           Insert each value from each node in stack in the heap -> Linearizability point, when the value is now part of the priority queue
 *           Once a node has been inserted, move to the next before linking the previous node to itself until we reach null
 *           Increment count
 *       Batch add count to size
 *   Release the lock and return true
 *
 * Here we could try to help the insert path as well by inserting nodes from the stack
 *  Head (Poll):
 *   Hold the lock
 *   Detach the head of then stack and then insert all values from the stack into the node
 *   Poll the head of the queue
 *   Release the lock
 *   Return the value seen
 *
 * Peek:
 *   Hold the lock
 *   Peek the head of the priority queue
 *   Release the lock
 * */
public class StagedConcurrentHeap<T extends Comparable<T>> implements Heap<T>{

    private final Lock lock;
    private final MPSCStack<T> stack;
    private final PriorityQueue<T> queue;
    private final AtomicInteger size;

    public StagedConcurrentHeap() {
        this.lock = new ReentrantLock();
        this.stack = new MPSCStack<>();
        this.queue = new PriorityQueue<>(Collections.reverseOrder());
        this.size = new AtomicInteger();
    }

    @Override
    public boolean add(T t) {
        Node<T> node = new Node<>(Objects.requireNonNull(t));
        Lock l = lock;
        MPSCStack<T> s = stack;
        s.casToHead(node); //Cas to head

        if (l.tryLock()) {
            PriorityQueue<T> q = queue;
            AtomicInteger i = size;
            try {
                insertToPQ(s, q, i);
            }finally {
                l.unlock();
            }
        }

        return false;
    }


    //Stale peeks are allowed as a relaxed invariant. However I could stricten the invariants
    @Override
    public T peek() {
        Lock l = lock;
        try {
            l.lock();
            return queue.peek();
        }finally {
            l.unlock();
        }
    }

    @Override
    public T poll() {
        Lock l = lock;
        MPSCStack<T> s = stack;
        try {
            l.lock();
            AtomicInteger i = size;
            PriorityQueue<T> q = queue;
            insertToPQ(s, q, i);
            T val = q.poll();
            if (val != null) i.decrementAndGet();
            return val;
        }finally {
            l.unlock();
        }

    }

    //Only inserted by lock holders
    void insertToPQ(MPSCStack<T> s, PriorityQueue<T> q, AtomicInteger i){
        Node<T> n = s.detachHead();
        Node<T> next;
        int count = 0;
        while (n != null) {
            q.add(n.value);
            ++count;
            next = n.loNext();
            n.spNext(n); //Set next to our selves, a plain write is alright here
            n = next;
        }
        i.addAndGet(count);
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public int capacity() {
        return Integer.MAX_VALUE;
    }

    @SuppressWarnings("unchecked")
    static class Node<T extends Comparable<T>> {
        volatile Node<T> next;
        final T value;

        public Node(T value) {
            this.value = value;
        }

        public Node<T> loNext() {
            return (Node<T>) NEXT.getAcquire(this);
        }

        //Backed by volatile write
        void spNext(Node<T> next) {
            NEXT.set(this, next);
        }
    }


    static class MPSCStack<T extends Comparable<T>> {
        volatile Node<T> head;

        void casToHead(Node<T> node) {
            Node<T> lhead;
            do {
                lhead = loHead();
                node.spNext(lhead);
            }while (!HEAD.compareAndSet(this, lhead, node)); //Volatile writes contain a release fence so next is always visible
        }


        //Returns the previous head, a volatile read and write. release ensures null write is visible immediately, acquire read ensures we always see each node's next pointer
        @SuppressWarnings("unchecked")
        Node<T> detachHead(){
            return (Node<T>) HEAD.getAndSet(this, null);
        }

        @SuppressWarnings("unchecked")
        Node<T> loHead() {
            return (Node<T>) HEAD.getAcquire(this);
        }
    }


    private static final VarHandle HEAD;
    private static final VarHandle NEXT;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            NEXT = l.findVarHandle(Node.class, "next", Node.class);
            HEAD = l.findVarHandle(MPSCStack.class, "head", Node.class);
        }catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}