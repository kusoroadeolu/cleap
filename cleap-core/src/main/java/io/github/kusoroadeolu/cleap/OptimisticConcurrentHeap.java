//package io.github.kusoroadeolu.cleap;
//
//import java.lang.invoke.MethodHandles;
//import java.lang.invoke.VarHandle;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Objects;
//import java.util.PriorityQueue;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//
//
//
///*
//* A different implementation of the staged prio queue
// * Insert:
// *   Initialize a new node object with our item on construction
// *   Then repeatedly try to cas our node to the head of the stack until we succeed
// *   Then try to acquire the lock, if we fail, return our result as false (we didnt get added immediately though we will later on)
// *   Otherwise
// *       Detach the stack from its head using an atomic getSet instruction, setting the current head as null
// *       Store a count variable
// *       Repeatedly
// *           If the node is null
// *           Insert each value from each node in stack in the heap, if the node is marked as deleted, skip it -> Linearizability point, when the value is now part of the priority queue
// *           Once a node has been inserted, move to the next
// *           Increment count
// *       Batch add count to size
// *   Release the lock and return true
// *
// * Here we could try to help the insert path as well by inserting nodes from the stack
// *  Head (Poll):
// *   Hold the lock
// *   Load(Don't detach) the head of the stack, scan through the stack sequentially to find the highest priority node in the stack
// *   Peek the head of the priority queue
// *   If the head has a higher priority than the node's item, poll the head and heapify the queue and decrement the size
// *   Else mark the node as deleted with a plain write and don't heapify the queue (don't decrement the size)
// *   Release the lock
// *   Return the highest priority value
// *
// * Peek:
// *   Hold the lock
// *   Scan the stack for the highest priority node, storing it as a local variable
// *   Peek the head of the priority queue
// *   If the head of the priority queue is a lower priority than stack node return the stack value (don't mark the node as deleted)
// *   otherwise increment the value
// *
// *
// * Polls and peeks are allowed to lag behind
// * */
//public class OptimisticConcurrentHeap<T extends Comparable<T>> implements Heap<T> {
//    private final Lock lock;
//    private final MPSCStack<T> stack;
//    private final PriorityQueue<T> queue;
//    private final AtomicInteger size;
//
//    public OptimisticConcurrentHeap() {
//        this(Collections.emptyList());
//    }
//
//    //Never call this, otherwise, nodes might never get inserted, this is just for stress tests
//    public OptimisticConcurrentHeap(Collection<T> collection) {
//        this.lock = new ReentrantLock();
//        this.stack = new MPSCStack<>();
//        this.queue = new PriorityQueue<>(Collections.reverseOrder());
//        this.size = new AtomicInteger();
//        for (T t : collection) {
//            stack.casToHead(new Node<>(t));
//        }
//    }
//
//    //Returns false if the element was not immediately added to the queue
//    @Override
//    public boolean add(T t) {
//        Node<T> node = new Node<>(Objects.requireNonNull(t));
//        Lock l = lock;
//        MPSCStack<T> s = stack;
//        s.casToHead(node); //Cas to head
//
//        if (l.tryLock()) {
//            PriorityQueue<T> q = queue;
//            AtomicInteger i = size;
//            try {
//                insertToPQ(s, q, i);
//            }finally {
//                l.unlock();
//            }
//        }
//
//        return false;
//    }
//
//
//    //Stale peeks aren't allowed here, though peeks can lag behind
//    @Override
//    public T peek() {
//        Lock l = lock;
//        MPSCStack<T> s = stack;
//        Node<T> hpNode = findHighestPriorityNode(s); //Fast path
//        l.lock();
//        if (hpNode != null && hpNode.isDead()) hpNode = findHighestPriorityNode(s); //Fast path
//        try {
//            PriorityQueue<T> q = queue;
//            T val = q.peek();
//
//            if (hpNode == null || hpNode.isApplied()) return val;
//
//            if (val == null || hpNode.value.compareTo(val) > 0) return hpNode.value;
//
//            return val;
//        }finally {
//            l.unlock();
//        }
//
//    }
//
//    /*
//    * If an inserter drains into the stack and the node we saw is at the head,
//    * it is wasted work, it doesn't affect correctness, just performance, the idea is to reduce the time spent under the lock.
//    * If the node we read outside isn't at the head of the queue, we don't mark the node as dead.
//    * Also the idea of does a higher priority node exist on the stack after we've scanned the stack under the lock exists on my current path.
//    * However to handle the case of when we see a node that has already been inserted into the queue.
//    * Only the insert path, we mark the node as APPLIED if it has already been inserted into the queue. If it is applied, we just pop the head of the queue
//    * */
//
//    @Override
//    public T poll() {
//        Lock l = lock;
//        MPSCStack<T> s = stack;
//        Node<T> hpNode = findHighestPriorityNode(s); //Fast path
//        l.lock();
//        if (hpNode != null && hpNode.isDead()) hpNode = findHighestPriorityNode(s); //Retry is our hpnode is dead, if it is applied, we just peek, no need to rescan
//
//        try {
//            PriorityQueue<T> q = queue;
//            T val = q.peek();
//
//            if (hpNode == null || hpNode.isApplied()) {
//                q.poll(); //Remove the head and heapify
//                if (val != null) size.decrementAndGet();
//                return val;
//            }
//
//            if (val == null || hpNode.value.compareTo(val) > 0) {
//                hpNode.state = Node.DEAD; //Mark as dead. As a later
//                return hpNode.value;
//            }
//
//            q.poll();
//            size.decrementAndGet();
//            return val;
//        }finally {
//            l.unlock();
//        }
//
//    }
//
//
//    //Skip this node
//    private Node<T> findHighestPriorityNode(MPSCStack<T> stack) {
//        Node<T> highest = null;
//        for (Node<T> curr = stack.loHead(); curr != null; curr = curr.loNext()) {
//            if (curr.isDead()) continue; //Skip dead nodes
//            T val = curr.value;
//            if (highest == null || val.compareTo(highest.value) > 0) highest = curr;
//        }
//
//        return highest;
//    }
//
//    //Only inserted by lock holders
//    void insertToPQ(MPSCStack<T> s, PriorityQueue<T> q, AtomicInteger i){
//        Node<T> n = s.detachHead();
//        Node<T> next;
//        int count = 0;
//        while (n != null) {
//            if (!n.isDead()) {
//                q.add(n.value);
//                n.state = Node.APPLIED;
//                ++count;
//            }
//
//            next = n.loNext();
//            n.soNextNull(); //Make visible to pollers traversing the queue w/o a lock
//            n = next;
//        }
//
//        i.addAndGet(count);
//    }
//
//    @Override
//    public int size() {
//        return size.get();
//    }
//
//    @Override
//    public int capacity() {
//        return Integer.MAX_VALUE;
//    }
//
//    @SuppressWarnings("unchecked")
//    static class Node<T extends Comparable<T>> {
//        volatile Node<T> next;
//        final T value;
//        int state = ALIVE; //Plain write will always be backed by a lock
//        static final int APPLIED = 2;
//        static final int ALIVE = 1;
//        static final int DEAD = 0;
//
//        public Node(T value) {
//            this.value = value;
//        }
//
//        public boolean isDead(){
//            return state == DEAD;
//        }
//
//        public Node<T> loNext() {
//            return (Node<T>) NEXT.getAcquire(this);
//        }
//
//        public Node<T> lpNext() {
//            return (Node<T>) NEXT.get(this);
//        }
//
//        public boolean isApplied() {
//           return state == APPLIED;
//        }
//
//        //Backed by volatile write
//        void spNext(Node<T> next) {
//            NEXT.set(this, next);
//        }
//
//        void soNextNull() {
//            NEXT.setRelease(this, null);
//        }
//
//
//        @Override
//        public String toString() {
//            return "Node[" +
//                    "next=" + next +
//                    ", value=" + value +
//                    ", state=" + state +
//                    ']';
//        }
//
//    }
//
//
//    static class MPSCStack<T extends Comparable<T>> {
//        volatile Node<T> head;
//
//        void casToHead(Node<T> node) {
//            Node<T> lhead;
//            do {
//                lhead = loHead();
//                node.spNext(lhead);
//            } while (!HEAD.compareAndSet(this, lhead, node)); //Volatile writes contain a release fence so next is always visible
//        }
//
//
//        //Returns the previous head, a set release, happens before the get acquire, so the ordered load from head should be viisible
//        @SuppressWarnings("unchecked")
//        Node<T> detachHead(){
//            return (Node<T>) HEAD.getAndSet(this, null);
//        }
//
//        @SuppressWarnings("unchecked")
//        Node<T> loHead() {
//            return (Node<T>) HEAD.getAcquire(this);
//        }
//
//    }
//
//
//    private static final VarHandle HEAD;
//    private static final VarHandle NEXT;
//
//    static {
//        try {
//            MethodHandles.Lookup l = MethodHandles.lookup();
//            NEXT = l.findVarHandle(Node.class, "next", Node.class);
//            HEAD = l.findVarHandle(MPSCStack.class, "head", Node.class);
//        }catch (ReflectiveOperationException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}

package io.github.kusoroadeolu.cleap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



/*
 * A different implementation of the staged prio queue
 * Insert:
 *   Initialize a new node object with our item on construction
 *   Then repeatedly try to cas our node to the head of the stack until we succeed
 *   Then try to acquire the lock, if we fail, return our result as true (since this heap is unbounded)
 *   Otherwise
 *       Detach the stack from its head using an atomic getSet instruction, setting the current head as null
 *       Store a count variable
 *       Repeatedly
 *           If the node is null
 *           Insert each value from each node in stack in the heap, if the node is marked as deleted, skip it -> Linearizability point, when the value is now part of the priority queue
 *           Once a node has been inserted, move to the next before linking the previous node to itself until we reach null
 *           Increment count
 *       Batch add count to size
 *   Release the lock and return true
 *
 * Here we could try to help the insert path as well by inserting nodes from the stack
 *  Head (Poll):
 *   Hold the lock
 *   Load(Don't detach) the head of the stack, scan through the stack sequentially to find the highest priority node in the stack
 *   Peek the head of the priority queue
 *   If the head has a higher priority than the node's item, poll the head and heapify the queue and decrement the size
 *   Else mark the node as deleted with a plain write and don't heapify the queue (don't decrement the size)
 *   Release the lock
 *   Return the highest priority value
 *
 * Peek:
 *   Hold the lock
 *   Scan the stack for the highest priority node, storing it as a local variable
 *   Peek the head of the priority queue
 *   If the head of the priority queue is a lower priority than stack node return the stack value (don't mark the node as deleted)
 *   otherwise increment the value
 *
 *
 * Polls and peeks are allowed to lag behind
 * */
public class OptimisticConcurrentHeap<T extends Comparable<T>> implements Heap<T> {
    private final Lock lock;
    private final MPSCStack<T> stack;
    private final PriorityQueue<T> queue;
    private final AtomicInteger size;

    public OptimisticConcurrentHeap() {
        this(Collections.emptyList());
    }

    public OptimisticConcurrentHeap(Collection<T> collection) {
        this.lock = new ReentrantLock();
        this.stack = new MPSCStack<>();
        this.queue = new PriorityQueue<>(Collections.reverseOrder());
        this.size = new AtomicInteger();
        for (T t : collection) {
            stack.casToHead(new Node<>(t));
        }
    }

    //Returns false if the element was not immediately added to the queue
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
                return true;
            }finally {
                l.unlock();
            }
        }

        return false;
    }


    //Stale peeks aren't allowed here, though peeks can lag behind
    @Override
    public T peek() {
        Lock l = lock;
        MPSCStack<T> s = stack;
        try {
            l.lock();
            PriorityQueue<T> q = queue;
            Node<T> hpNode = findHighestPriorityNode(s);
            T val = q.peek();
            if (hpNode == null) return val;
            if (val == null) return hpNode.value;
            if (hpNode.value.compareTo(val) > 0) return hpNode.value;
            return val;

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
            PriorityQueue<T> q = queue;
            Node<T> hpNode = findHighestPriorityNode(s);
            T val = q.peek();

            if (hpNode == null) {
                q.poll(); //Remove the head
                if (val != null) size.decrementAndGet();
                return val;
            }

            if (val == null || hpNode.value.compareTo(val) > 0) {
                hpNode.state = Node.DEAD; //Mark as dead
                return hpNode.value;
            }

            q.poll();
            size.decrementAndGet();
            return val;
        }finally {
            l.unlock();
        }

    }

    private Node<T> findHighestPriorityNode(MPSCStack<T> stack) {
        Node<T> highest = null;
        for (Node<T> curr = stack.loHead(); curr != null; curr = curr.loNext()) {
            if (curr.isDead()) continue; //Skip dead nodes
            T val = curr.value;
            if (highest == null || val.compareTo(highest.value) > 0) highest = curr;
        }

        return highest;
    }

    //Only inserted by lock holders
    void insertToPQ(MPSCStack<T> s, PriorityQueue<T> q, AtomicInteger i){
        Node<T> n = s.detachHead();
        Node<T> next;
        int count = 0;
        while (n != null) {
            if (!n.isDead()) {
                q.add(n.value);
                ++count;
            }

            next = n.loNext();
            n.spNext(n); //Set next to our selves, a plain write is alright here as this stack is essentially detached
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
        int state = ALIVE; //Plain write will always be backed by a lock
        static final int ALIVE = 1;
        static final int DEAD = 0;

        public Node(T value) {
            this.value = value;
        }

        public boolean isDead(){
            return state == DEAD;
        }

        public Node<T> loNext() {
            return (Node<T>) NEXT.getAcquire(this);
        }

        //Backed by volatile write
        void spNext(Node<T> next) {
            NEXT.set(this, next);
        }

        @Override
        public String toString() {
            return "Node[" +
                    "next=" + next +
                    ", value=" + value +
                    ", state=" + state +
                    ']';
        }
    }


    static class MPSCStack<T extends Comparable<T>> {
        volatile Node<T> head;

        void casToHead(Node<T> node) {
            Node<T> lhead;
            do {
                lhead = loHead();
                node.spNext(lhead);
            } while (!HEAD.compareAndSet(this, lhead, node)); //Volatile writes contain a release fence so next is always visible
        }


        //Returns the previous head, a set release, happens before the get acquire, so the ordered load from head should be viisible
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
