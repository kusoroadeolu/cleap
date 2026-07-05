package io.github.kusoroadeolu.cleap.bounded;

import io.github.kusoroadeolu.cleap.Heap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ElimLBBoundedPQ<T> implements Heap<T> {
    private final InsertArray<T> insertArray;
    private final Comparator<T> nullReverseComparator; //Packs the bottom of an array with nulls
    private volatile DeleteArray<T> deleteArray;
    private final int capacity;
    private final int maxDaCapacity;
    private final int slack;
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    public ElimLBBoundedPQ(int capacity) {
        this.capacity = capacity;
        this.maxDaCapacity = Math.max(1, (int) (0.1 * capacity));
        this.insertArray = new InsertArray<>(capacity);
        this.deleteArray = new DeleteArray<>(0);
        this.slack = Math.max(1, (int) (0.1 * maxDaCapacity));
        this.nullReverseComparator = (a, b) -> {
            if (b == null || a == null) return 0;
            return compare(b, a);
        };
    }

    public ElimLBBoundedPQ(int capacity, int slack) {
        this.capacity = capacity;
        this.maxDaCapacity = Math.max(1, (int) (0.1 * capacity));
        this.insertArray = new InsertArray<>(capacity);
        this.deleteArray = new DeleteArray<>(0);
        this.slack = slack;
        this.nullReverseComparator = (a, b) -> {
            if (b == null || a == null) return 0;
            return compare(b, a);
        };
    }


    @Override
    public boolean add(T t) {
        var ia = insertArray;
        var lock = ia.rwLock.readLock();
        int capacity = this.capacity;
        lock.lock();
        try {
            int index = ia.loIndex();

            if (index == capacity) return false;
            var da = loDArr();
            int daIndex;
            int daCap;
            for (;;) {
                //70, 30
                //71, 30 -0

                daIndex = da.dIndex; // (0, 1, 2, 3), if index is on 2, that means value at 2 hasn't been consumed yet
                daCap = da.capacity; //10 - 5 = 5
                int next = index + 1;
                int total = index + (daCap - daIndex);

                if (total >= capacity) return false;
                else if (I_INDEX.compareAndSet(ia, index, next)) break;
                else index = ia.loIndex(); //re-read
            }

            ia.items[index] = t;
            if (da.capacity == 0 || daIndex == daCap) return true; //nothing in the D.A, we'll get merged on next delete min
            var lastDaIndex = da.capacity - 1;
            int prio = compare(t, da.items[lastDaIndex]);
            if (prio < 0) MERGE.getAndAdd(da, 1); //Writes to items is made visible by da status read
            return true;
        }finally {
            lock.unlock();
        }
    }

    static final int MAX_SPINS = 1000;

    @Override
    public T poll() {
        var ia = insertArray;
        outer: for (;;) {
            var da = deleteArray;
            int daCap;
            int iIndex;
            int daIndex;
            var lock = da.lock;
            Result result = new Result();

            for (;;) {
                if (da.state == State.DEAD) continue outer;

                daCap = da.capacity;
                iIndex = ia.loIndex();
                daIndex = da.dIndex;
                boolean noElems = daIndex == daCap;
                boolean isEmpty = noElems && iIndex == 0;

                if (isEmpty) return null;

                var arena = da.array;
                if (lock.getAcquire() == DeleteArray.FREE && lock.getAndIncrement() == DeleteArray.FREE) {
                    daIndex = (int) D_INDEX.get(da);
                    iIndex = ia.loIndex();

//                    noElems = daIndex == daCap;
//                    isEmpty = noElems && iIndex == 0;
//
//                    if (isEmpty) return null;

                    T t = null;
                    if (noElems || da.mergeCount >= slack) {
                        System.out.println("Before merge");
                        da = merge(ia, da);
                        daIndex = 0;
                        lock = da.lock;
                        System.out.println("After merge");
//                        t = da.items[0];
//                        D_INDEX.setRelease(da, 1); //Made visible by write to state (for deletes) and lock for inserts
//                        lock = da.lock;
//                        daIndex = 1;
                    } else{
                        t = da.items[daIndex++];
                        D_INDEX.setRelease(da, daIndex);
                    }

                    for (int i = 0; i < NCPU; ++i) {
                        var r = arena.getAcquire(i);
                        if (r == null || !arena.compareAndSet(i, r ,Result.waiting())) continue;

                        if (daIndex == da.capacity) {
                            if (ia.loIndex() == 0) r.item = Result.empty();
                            else r.item = Result.retry();
                        } else {
                            r.item = da.items[daIndex++];
                            D_INDEX.setRelease(da, daIndex);
                        }

                        arena.setRelease(i, null);
                    }
                    lock.setRelease(DeleteArray.FREE);
                    return t;
                }

//                int half = NCPU / 2;
//                int slotSpins = MAX_SPINS / half;
//                inner: for (int i = 0 ; i < half; ++i) {
//                    if (da.state == State.DEAD) continue outer;
//
//                    int index = ThreadLocalRandom.current().nextInt(NCPU);
//                    int spins = 0;
//
//                    if (!arena.compareAndSet(index, null ,result)) continue;
//
//                    for (;;) {
//                        if (++spins >= slotSpins) {
//                            if (arena.compareAndSet(index, result, null)) continue inner;
//
//                            while (arena.getAcquire(index) == Result.waiting() && result.item == null) Thread.onSpinWait();
//
//                            if (result.item == Result.retry()) {
//                                result.item = null;
//                                continue outer;
//                            }
//
//                            return result.item == Result.empty() ? null : (T) result.item;
//                        } else if (arena.getAcquire(index) != result || result.item != null) {
//
//                            while (arena.getAcquire(index) == Result.waiting() && result.item == null) Thread.onSpinWait();
//
//
//                            if (result.item == Result.retry()) {
//                                result.item = null;
//                                continue outer;
//                            }
//
//                            return result.item == Result.empty() ? null : (T) result.item;
//                        }
//                        //Need to include these extra result checks otherwise, we might loop 4ever
//
//                        Thread.onSpinWait();
//                    }
//                }

            }
        }
    }

    DeleteArray<T> merge(InsertArray<T> ia, DeleteArray<T> da) {
        try {
            var lock = ia.rwLock.writeLock();
            lock.lock();
            try {
                var iItems = ia.items;
                var dItems = da.items;
                var cmp = nullReverseComparator;
                var mda = maxDaCapacity;
                int iIndex = ia.lpIndex(); //start index for insert array (val at this index is always null)
                int dIndex = da.dIndex; //start index for delete array
                int dSize = da.capacity;
                for (int i = dIndex; i < dSize; ++i) {
                    iItems[iIndex++] = dItems[dIndex];
                }

                Arrays.sort(iItems, cmp);

                int capacity = Math.min(iIndex, mda);
                var newDa = new DeleteArray<T>(capacity);
                for (int i = iIndex - 1, j = 0; j < capacity; --i, j++) {
                    var v = iItems[i];
                    newDa.items[j] = v;
                    iItems[i] = null;
                }

                //increment to ensure no one can hold this acquire this after we make this visible.
                // Write to zero will be made visible by write to state

                //For inserts backed by exclusive lock, otherwise for deletes and inserts, backed by status write

                newDa.lock.setPlain(0);
                D_ARR.set(this, newDa);
                I_INDEX.set(ia, Math.max(0, iIndex - mda));
                da.state = State.DEAD;
                return newDa;
            }finally {
                lock.unlock();
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public List<T> toList() {
        var da = deleteArray;
        var ia = insertArray;
        List<T> list = new ArrayList<>();

        ia.rwLock.readLock().lock();
        try {
            for (int i = da.dIndex; i < da.capacity; ++i) {
                list.add(da.items[i]);
            }

            int upto = ia.index;
            for (int i = 0; i < upto; ++i) {
                list.add(ia.items[i]);
            }

        }finally {
            ia.rwLock.readLock().unlock();
        }

        return list;
    }

    DeleteArray<T> loDArr() {
        return (DeleteArray<T>) D_ARR.getAcquire(this);
    }



    InsertArray<T> insertArray() {
        return insertArray;
    }

    public DeleteArray<T> deleteArray() {
        return deleteArray;
    }

    @Override
    public T peek() {
        return null;
    }

    int compare(T a, T b) {
        return ((Comparable<T>) a).compareTo(b);
    }


    @Override
    public int size() {
        var da = deleteArray;
        return insertArray.loIndex() + da.size();
    }

    @Override
    public void clear() {
        var ia = insertArray;
        ia.rwLock.writeLock().lock();
        try {
            for (;;) {
                if (poll() == null) break;
            }
        }finally {
            ia.rwLock.writeLock().unlock();
        }
    }

    public static class DeleteArray<T> {
        static final int FREE = -1;
        volatile State state = State.NONE;
        public final T[] items;
        final int capacity;
        final AtomicInteger lock = new AtomicInteger(FREE);
        final AtomicReferenceArray<Result> array = new AtomicReferenceArray<>(NCPU);

        volatile int dIndex;
        volatile int mergeCount;

        DeleteArray(int capacity) {
            if (capacity == 0) items = null;
            else items = (T[]) new Object[capacity];
            this.capacity = capacity;

        }

        int size() {
            return capacity - dIndex;
        }
    }

    enum State{NONE, DEAD}

    static class Result {
        Object item;

        static final Object RETRY = new Object();
        static final Object EMPTY = new Object();
        static final Result WAITING = new Result();

        static Object retry() {
            return RETRY;
        }

        static Result waiting() {
            return WAITING;
        }

        static Object empty() {
            return EMPTY;
        }

        @Override
        public String toString() {
            var i = this.item;
            String item;
            if (i == RETRY) item = "RETRY";
            else if (i == WAITING) item = "WAITING";
            else if (i == EMPTY) item = "EMPTY";
            else if (i == null) item = "null";
            else item = this.item.toString();

            return "Result[" +
                    "item=" + item +
                    ']';
        }
    }

    static class InsertArray<T> {
        private final T[] items;
        private volatile int index;
        private final ReadWriteLock rwLock;

        public InsertArray(int capacity) {
            this.items = (T[]) new Object[capacity];
            this.rwLock = new ReentrantReadWriteLock();
        }

        public InsertArray(T[] items, int index) {
            this.items = items;
            this.index = index;
            this.rwLock = new ReentrantReadWriteLock();
        }

        int loIndex() {
            return (int) I_INDEX.getAcquire(this);
        }

        int lpIndex() {
            return (int) I_INDEX.get(this);
        }
    }

    private static final VarHandle I_INDEX;
    private static final VarHandle D_INDEX;
    private static final VarHandle D_ARR;
    private static final VarHandle MERGE;

    static {
        var l = MethodHandles.lookup();
        try{
            I_INDEX = l.findVarHandle(InsertArray.class, "index", int.class);
            D_INDEX = l.findVarHandle(DeleteArray.class, "dIndex", int.class);
            D_ARR = l.findVarHandle(ElimLBBoundedPQ.class, "deleteArray", DeleteArray.class);
            MERGE = l.findVarHandle(DeleteArray.class, "mergeCount", int.class);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
