package io.github.kusoroadeolu.cleap;


import io.github.kusoroadeolu.cleap.LeaderList.Node;
import io.github.kusoroadeolu.cleap.LeaderList.WQNode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
* A segmented priority queue. This priority queue is split into two levels, a leader level and a worker level
*
* The leader level with a shared global ordered lock free list while the worker level consists of a lazily initialized shared array of priority queues (worker pqs)
*
* The shared list keeps track of the top N highest priority items per array index capped at a maximum and restricted to a minimum (assuming there are nodes in the worker pq)
* Deletes on this shared list are serialized while up to N number of inserts can happen concurrently(meaning per worker pq, inserts are also serialized)
*
* Ideally the paper which this was adapted from assumes segmentation through numa nodes (i.e. a pre allocated array index for threads running on a numa node)
* However, since we don'item have this luxury, during inserts threads randomly select an array index to work on. To reduce threads waiting on array indexes,
* we shrink or increase the range of the array of which we can work on when we fail to acquire a lock after 2 tries
*
*
* Compared to the original PIPQ paper, this implementation is pretty different though shares the same structural similarities
*
* In the original paper, delete mins were serialized by a delete min lock, though this happened independently of worker heap locks.
* Delete mins on a segment (or numa node as referred to in the paper), happened outside a lock, while this seemed fine, from my reasoning
* this could cause an issue where regarding largest list value (llv) of a segment if the segment counter was one though the llv of a segment was
* logically deleted outside which could cause for stale values regarding the llv, if multiple insertions followed,
* before the delete operation could acquire the lock to update the stale value. A fix proposed was to store the node not the value itself and force threads to
* read the status of a node before comparing (if its deleted, we re-search the list for the new llv otherwise,we don'item).
* However I didn'item do it because honestly the llv should be updated under the lock to prevent weird interleavings
*
*
* Another issue i came across is one where the counter of a segment could be decremented twice (rather than once), when an inserting thread upserts a value when segment.count == CNTR_MAX
* This could occur if the segment value didn'item check if the llv had been logically deleted before add the llv to the queue after moving it downwards
* */
public class PIPQ<T> implements Heap<T> {

    private final int ncpu;
    private final WorkerPQSegment<T>[] workerQueues;
    private final LeaderList<T> leaderList;
    private volatile int bound; //max array pos
    private final Comparator<T> comparator;
    private final SegmentCoordinatorLocks locks;
    private final AnnouncementArena<T> announcements;

    private static final int MAX_MISSES = 5;
    private static final int MAX_SPINS = 500;
    private static final int CNTR_MAX = 100;
    private static final int CNTR_MIN = 5;

    private static final Object CLAIMED = new Object();
    private static final Object DONE = new Object();

    public PIPQ(Comparator<T> comparator) {
        ncpu = Runtime.getRuntime().availableProcessors();
        workerQueues = new WorkerPQSegment[ncpu];

        for (int i = 0; i < ncpu; ++i) {
            workerQueues[i] = new WorkerPQSegment<>();
        }

        bound = 1;
        this.comparator = comparator;
        locks = new SegmentCoordinatorLocks(ncpu);
        announcements = new AnnouncementArena<>(ncpu);
        leaderList = new LeaderList<>(comparator);
    }

    public PIPQ() {
        this(null);
    }

    public int bound() {
        return bound;
    }

    /*
    *
    * */
    @Override
    public boolean add(T t) {
        var wqs = workerQueues;
        var cmp = comparator;

        for (;;) {
            int b = bound;
            int segment = ThreadLocalRandom.current().nextInt(b);
            var wq = wqs[segment];
            int failed = 0;

            while (failed < MAX_MISSES) {
                if (wq.tryLock(5)) {
                    try {
                        int size = wq.size();
                        var qMin = wq.peekMin();
                        if (size == 0 || compare(t, qMin, cmp) < 0) {
                            int count = wq.segmentListCount();
                            var llv = wq.largestListValue;
                            var ll = leaderList;
                            if (count == CNTR_MAX) {
                                boolean isMarked = false;
                                if (llv == null || (isMarked = llv.isMarked())) {
                                    wq.largestListValue = ll.findListLargest(null, segment);
                                    llv = wq.largestListValue;
                                }

                                int res = compare(t, llv.item.t(), cmp);
                                if (res > 0 && !isMarked) wq.add(t); //Lower prio tha
                                else if (res > 0) {
                                    wq.largestListValue = ll
                                            .insertAndReturnLargestSegmentNode(t, llv.item.t(), segment)
                                            .item();
                                }
                                else shiftUp(wq, t, llv.item.t(), segment);


                            } else {
                                ll.insert(t, segment);
                                wq.incrementListCount();
                            }
                        } else wq.add(t);

                        return true;
                    }finally {
                        wq.unlock();
                    }
                }

                ++failed;
            }

            if (b != ncpu) BOUND.compareAndSet(this, b, b + 1);
        }
    }

    @Override
    public T poll() {
        var wqs = workerQueues;
        var ll = leaderList;

        for (;;) {
            var head = ll.poll();

            if (head == null) return null;

            int segment = head.item.segment();
            var wq = wqs[segment];
            int cnt = wq.decrementListCount();

            if (cnt <= CNTR_MIN){
                wq.lock();
                try {
                    int count = wq.segmentListCount();
                    if (count < CNTR_MIN) {
                        T min = wq.deleteMin();
                        if (min == null && count == 0) { //if queue is empty and there's no value in the list
                            int b; //bound is 1 indexed, segment is zero indexed
                            if (segment > 0 && (b = bound) == (segment + 1)) BOUND.compareAndSet(this, b, b - 1); //if this is the greatest segment bound, try to decrease
                            wq.largestListValue = null;
                        } else if (min != null) {
                            ll.insert(min, segment);
                            wq.incrementListCount();
                        }
                    }

                    return head.item.t();
                }finally {
                    wq.unlock();
                }
            }

        }
    }

    public void clear() {
        leaderList.clear();
        for (int i =0; i < ncpu; ++i) {
            var wq = workerQueues[i];
            wq.lock();
            try {
                wq.queue.clear();
                wq.largestListValue = null;
                wq.segmentCount = 0;
            }finally {
                wq.unlock();
            }
        }
    }

    void idle() {
        for (int i = 0; i < MAX_SPINS; ++i) Thread.onSpinWait();
    }

    void shiftUp(WorkerPQSegment<T> wq, T t, T llv, int segment) {
        var ls = leaderList;
        var mvResult = ls.insertAndReturnLargestSegmentNode(t, llv, segment);

        if (mvResult.marked()) wq.add(llv);
        wq.largestListValue = mvResult.item();

    }

    int compare(T k, T t, Comparator<T> cmp) {
        if (t == null) return 0;
        if (cmp == null) {
            Comparable<T> c = (Comparable<T>) k;
            return c.compareTo(t);
        } else return cmp.compare(k, t);
    }

    @Override
    public T peek() {
        return null;
    }


    @Override
    public int size() {
        return 0;
    }

    @Override
    public int capacity() {
        return Integer.MAX_VALUE;
    }


    private static class WorkerPQLPad {
        long l1, l2, l3, l4, l5, l6, l7, l8;
    }

    private static class WorkerPQFields<T> extends WorkerPQLPad{
        final PriorityQueue<T> queue;
        final Lock lock;
        Node<WQNode<T>> largestListValue; //Lowest prio value in the list for this pq
        volatile int segmentCount; //Number of values related to this segment in the shared list

        public WorkerPQFields(Comparator<T> comparator) {
            queue = new PriorityQueue<>(1, comparator);
            lock = new ReentrantLock();
        }

        public WorkerPQFields() {
            this(null);
        }
    }

    static class WorkerPQSegment<T> extends WorkerPQFields<T>{
        long l1, l2, l3, l4, l5, l6, l7, l8;

        public void lock() {
            lock.lock();
        }

        public boolean tryLock() {
            return lock.tryLock();
        }

        public boolean tryLock(long ns) {
            try {
                return lock.tryLock(ns, TimeUnit.NANOSECONDS);
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        void add(T t) {
            queue.offer(t);
        }

        T deleteMin() {
            return queue.poll();
        }

        T peekMin() {
            return queue.peek();
        }

        public void unlock() {
            lock.unlock();
        }

        public int incrementListCount() {
            return (int) SEGMENT_COUNT.getAndAdd(this, 1);
        }

        public int decrementListCount() {
            return (int) SEGMENT_COUNT.getAndAdd(this, -1);
        }

        public int segmentListCount() {
            return (int) SEGMENT_COUNT.getAcquire(this);
        }

        public int size() {
            return queue.size();
        }
    }

    static class Announcement<T> {
        T value;
    }


    static class AnnouncementArena<T> {
        private final AtomicReferenceArray<Announcement<T>> annoucements;
        private final AtomicInteger seq;
        private final int len;
        private static final int FREE = -1;

        public AnnouncementArena(int len) {
            this.len = len;
            this.annoucements = new AtomicReferenceArray<>(len);
            this.seq = new AtomicInteger(FREE);
        }

        public void set(int i, Announcement<T> announcement) {
            annoucements.setRelease(i, announcement);
        }

        public boolean cas(int i, Announcement<T> announcement) {
           return annoucements.compareAndSet(i,  (Announcement<T>) DONE, announcement);
        }

        boolean isFree() {
            return seq.getAcquire() == FREE;
        }

        void release() {
            seq.setRelease(FREE);
        }

        int nextIndex() {
            return seq.getAndIncrement();
        }

        public void setDone(int i) {
            set(i, (Announcement<T>) DONE);
        }

        public Announcement<T> get(int i) {
            return annoucements.getAcquire(i);
        }

        public void resetSeq() {
            seq.setRelease(0);
        }

        public boolean tryClaim(int i, Announcement<T> seen) {
            if (seen == null) return false;
            return annoucements.compareAndSet(i, seen,  (Announcement<T>) CLAIMED);
        }
    }

    static class SegmentCoordinatorLocks {
        private final AtomicReferenceArray<SpinLock> leaderLocks;

        public SegmentCoordinatorLocks(int ncpu) {
            this.leaderLocks = new AtomicReferenceArray<>(ncpu);
            for (int i  = 0; i < ncpu; ++i) {
                leaderLocks.set(i, new SpinLock());
            }

        }

        public boolean tryAcquire(int segment) {
            return leaderLocks.getAcquire(segment).tryLock();
        }

        public void release(int segment) {
            leaderLocks.getAcquire(segment).release();;
        }
    }

    static class SpinLock {
        volatile int status;
        static final int HELD = 1;
        static final int FREE = 0;

        boolean tryLock() {
            return (int) STATUS.getAcquire(this) == FREE && STATUS.compareAndSet(this, FREE, HELD);
        }

        void release() {
            STATUS.setRelease(this, FREE);
        }
    }

    private static final VarHandle SEGMENT_COUNT;
    private static final VarHandle STATUS;
    private static final VarHandle BOUND;

    static {
        try {
            var l = MethodHandles.lookup();
            SEGMENT_COUNT = l.findVarHandle(WorkerPQFields.class, "segmentCount", int.class);
            BOUND = l.findVarHandle(PIPQ.class, "bound", int.class);
            STATUS = l.findVarHandle(SpinLock.class, "status", int.class);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
